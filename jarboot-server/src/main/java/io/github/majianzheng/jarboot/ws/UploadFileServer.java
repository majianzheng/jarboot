package io.github.majianzheng.jarboot.ws;

import io.github.majianzheng.jarboot.common.utils.AesUtils;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.dao.FileUploadProgressDao;
import io.github.majianzheng.jarboot.entity.FileUploadProgress;
import io.github.majianzheng.jarboot.service.ServerRuntimeService;
import io.github.majianzheng.jarboot.utils.CommonUtils;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 文件上传服务
 * @author mazheng
 */
@ServerEndpoint("/jarboot/upload/ws")
@RestController
public class UploadFileServer {
    private static final Logger logger = LoggerFactory.getLogger(UploadFileServer.class);
    private static final Map<String, SessionProxy> SESSION_PROXY_MAP = new ConcurrentHashMap<>(16);
    private static FileUploadProgressDao fileUploadProgressDao;
    private static ServerRuntimeService serverRuntimeService;
    private FileUploadProgress fileUploadProgress;
    private FileOutputStream outputStream;
    private final AtomicBoolean scheduling = new AtomicBoolean(false);
    private boolean pause = false;
    private boolean importService = false;
    private File dstFile;

    @Autowired
    public void setFileUploadProgressDao(FileUploadProgressDao dao) {
        fileUploadProgressDao = dao;
    }
    @Autowired
    public void setServerRuntimeService(ServerRuntimeService s) {
        serverRuntimeService = s;
    }
    @OnOpen
    public void onOpen(Session session) {
        String clusterHost = CommonUtils.getSessionClusterHost(session);
        if (CommonUtils.needProxy(clusterHost)) {
            SESSION_PROXY_MAP.put(clusterHost, new SessionProxy(session, clusterHost));
            return;
        }
        String md5 = CommonUtils.getSessionParam("md5", session);
        String filename = CommonUtils.getSessionParam("filename", session);
        String relativePath = CommonUtils.getSessionParam("relativePath", session);
        long totalSize = Long.parseLong(CommonUtils.getSessionParam("totalSize", session));
        String baseDir = CommonUtils.getSessionParam("baseDir", session);
        String dstPath = CommonUtils.getSessionParam("dstPath", session);
        this.importService = StringUtils.isNotEmpty(CommonUtils.getSessionParam("importService", session));
        if (StringUtils.isEmpty(baseDir)) {
            baseDir = SettingUtils.getHomePath();
        } else {
            baseDir = AesUtils.decrypt(baseDir);
        }
        boolean append = true;
        dstFile = FileUtils.getFile(baseDir, dstPath);
        fileUploadProgress = fileUploadProgressDao.getFileUploadProgressByDstPath(dstFile.getAbsolutePath());
        if (null == fileUploadProgress) {
            FileUploadProgress temp = new FileUploadProgress();
            temp.setMd5(md5);
            temp.setDstPath(dstFile.getAbsolutePath());
            temp.setFilename(filename);
            temp.setRelativePath(relativePath);
            temp.setTotalSize(totalSize);
            temp.setUploadSize(0L);
            fileUploadProgress = fileUploadProgressDao.save(temp);
            if (dstFile.isFile() && dstFile.exists()) {
                // 已经存在的文件，覆盖
                logger.info("文件({})已经存在，将覆盖当前文件", dstFile.getAbsolutePath());
                append = false;
            }
        } else {
            if (dstFile.isFile() && dstFile.exists()) {
                logger.info("文件({})已经存在，当前文件大小：{}, 已上传大小：{}，总大小：{}",
                        dstFile.getAbsolutePath(),
                        dstFile.length(),
                        fileUploadProgress.getUploadSize(),
                        fileUploadProgress.getTotalSize());
                if (Objects.equals(dstFile.length(), fileUploadProgress.getUploadSize())) {
                    // 已经上传的文件实际大小与上传大小一致
                    append = Objects.equals(md5, fileUploadProgress.getMd5());
                } else {
                    append = false;
                }
            } else {
                append = false;
            }
        }
        if (!append) {
            fileUploadProgress.setUploadSize(0L);
            fileUploadProgress.setTotalSize(totalSize);
            fileUploadProgress.setMd5(md5);
            fileUploadProgressDao.save(fileUploadProgress);
        }
        try {
            outputStream = FileUtils.openOutputStream(dstFile, append);
            logger.info("开始上传文件：{}，append：{}, total: {}，uploadSize：{}", filename, append, totalSize, fileUploadProgress.getUploadSize());
            this.updateProgress(session);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            this.fileUploadProgress.setErrorMsg(e.getMessage());
            this.updateProgress(session);
        }
    }
    @OnClose
    public void onClose(Session session) {
        String clusterHost = CommonUtils.getSessionClusterHost(session);
        if (CommonUtils.needProxy(clusterHost)) {
            SessionProxy sessionProxy = SESSION_PROXY_MAP.remove(clusterHost);
            if (null != sessionProxy) {
                sessionProxy.proxyOnClose();
            }
            return;
        }
        try {
            if (null != fileUploadProgress && null != fileUploadProgress.getId()) {
                fileUploadProgressDao.save(fileUploadProgress);
            }
            if (this.importService && null != fileUploadProgress && Objects.equals(fileUploadProgress.getUploadSize(), fileUploadProgress.getTotalSize())) {
                // 导入服务处理
                serverRuntimeService.recoverService(session.getUserPrincipal().getName(), dstFile);
            }
            if (null == outputStream) {
                return;
            }
            outputStream.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    @OnError
    public void onError(Throwable error, Session session) {
        logger.debug(error.getMessage(), error);
        String clusterHost = CommonUtils.getSessionClusterHost(session);
        SessionProxy sessionProxy = SESSION_PROXY_MAP.remove(clusterHost);
        if (null != sessionProxy) {
            sessionProxy.proxyOnClose();
        }
        this.onClose(session);
    }

    @OnMessage
    public void onBinaryMessage(byte[] message, Session session) {
        if (null == outputStream || message.length == 0) {
            logger.info("已经传输完成，message size: {}", message.length);
            return;
        }
        if (pause) {
            logger.info("已经发生了异常，暂停");
            return;
        }
        String clusterHost = CommonUtils.getSessionClusterHost(session);
        if (CommonUtils.needProxy(clusterHost)) {
            SESSION_PROXY_MAP.get(clusterHost).proxyBinary(message);
            return;
        }
        try {
            outputStream.write(message);
            this.fileUploadProgress.setUploadSize(this.fileUploadProgress.getUploadSize() + message.length);
        } catch (Exception e) {
            this.fileUploadProgress.setErrorMsg(e.getMessage());
            this.pause = true;
            this.updateProgress(session);
        }
    }
    private void updateProgress(Session session) {
        if (scheduling.compareAndSet(false, true)) {
            try {
                if (null != outputStream) {
                    if (Objects.equals(fileUploadProgress.getUploadSize(), fileUploadProgress.getTotalSize())) {
                        outputStream.close();
                        outputStream = null;
                    } else {
                        outputStream.flush();
                    }
                }
                session.getBasicRemote().sendText(JsonUtils.toJsonString(this.fileUploadProgress));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                scheduling.set(false);
            }
        }
    }
}
