package io.github.majianzheng.jarboot.service.impl;

import io.github.majianzheng.jarboot.api.exception.JarbootRunException;
import io.github.majianzheng.jarboot.api.pojo.ServerRuntimeInfo;
import io.github.majianzheng.jarboot.base.AgentManager;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.common.CacheDirHelper;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.notify.FrontEndNotifyEventType;
import io.github.majianzheng.jarboot.common.pojo.ResultCodeConst;
import io.github.majianzheng.jarboot.common.utils.AesUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.common.utils.VersionUtils;
import io.github.majianzheng.jarboot.common.utils.ZipUtils;
import io.github.majianzheng.jarboot.entity.User;
import io.github.majianzheng.jarboot.service.ServerRuntimeService;
import io.github.majianzheng.jarboot.service.UserService;
import io.github.majianzheng.jarboot.utils.CommonUtils;
import io.github.majianzheng.jarboot.utils.MessageUtils;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import io.github.majianzheng.jarboot.utils.TaskUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author mazheng
 */
@Service
public class ServerRuntimeServiceImpl implements ServerRuntimeService {
    @Value("${docker:false}")
    private boolean isInDocker;
    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ServerRuntimeInfo getServerRuntimeInfo() {
        ServerRuntimeInfo info = new ServerRuntimeInfo();
        info.setMachineCode(CommonUtils.getMachineCode());
        info.setUuid(SettingUtils.getUuid());
        info.setInDocker(isInDocker);
        info.setVersion(VersionUtils.version);
        info.setHost(ClusterClientManager.getInstance().getSelfHost());
        info.setWorkspace(AesUtils.encrypt(SettingUtils.getWorkspace()));
        return info;
    }

    @Override
    public String getUuid() {
        return SettingUtils.getUuid();
    }

    @Override
    public void exportService(String name, OutputStream os) {
        if (StringUtils.isEmpty(name)) {
            throw new JarbootException(ResultCodeConst.EMPTY_PARAM, "导出失败，服务名为空！");
        }
        File dir = FileUtils.getFile(SettingUtils.getServicePath(name));
        if (!dir.exists()) {
            throw new JarbootException(ResultCodeConst.NOT_EXIST, "服务不存在！");
        }
        //创建缓冲输入流
        try {
            ZipUtils.toZip(dir, os, true);
        } catch (Exception e) {
            throw new JarbootException(e);
        }
    }

    @Override
    public void importService(String filename, InputStream file) {
        //临时目录，用于操作ZIP文件
        String name = StringUtils.stripEnd(filename, ".zip");
        final File tempDir = CacheDirHelper.getTempDir(name);
        if (tempDir.exists()) {
            //文件正在处理中
            throw new JarbootException("文件" + filename + "正在处理中...");
        }
        final File zipFie = FileUtils.getFile(tempDir, filename);
        try (OutputStream os = FileUtils.openOutputStream(zipFie)){
            FileUtils.forceMkdir(tempDir);
            //保持本地临时文件
            IOUtils.copy(file, os);
            String userDir = SettingUtils.getCurrentUserDir();
            TaskUtils.getTaskExecutor().execute(() -> doPushServer(userDir, tempDir, zipFie));
        } catch (Exception e) {
            throw new JarbootRunException(e.getMessage(), e);
        }
    }

    @Override
    public void recoverService(String username, File serviceZip) {
        String name = StringUtils.stripEnd(serviceZip.getName(), ".zip");
        final File tempDir = CacheDirHelper.getTempDir(name);
        User user = userService.findUserByUsername(username);
        String userDir = user.getUserDir();
        TaskUtils.getTaskExecutor().execute(() -> doPushServer(userDir, tempDir, serviceZip));
    }

    @Override
    public void downloadAnyFile(String encodedFilePath, OutputStream os) {
        //待下载文件名
        String fileName = AesUtils.decrypt(encodedFilePath);
        File target = FileUtils.getFile(fileName);
        if (!target.exists() || !target.isFile()) {
            throw new JarbootException(404, "文件不存在！" + fileName);
        }
        //创建缓冲输入流
        try (FileInputStream fis = new FileInputStream(target);){
            IOUtils.copy(fis, os);
        } catch (Exception e) {
            throw new JarbootException(e);
        }
    }

    private void doPushServer(String userDir, File tempDir, File zipFie) {
        final String id = tempDir.getName();
        //开始正式导入
        try {
            final File out = FileUtils.getFile(tempDir, "out");
            if (!out.exists()) {
                FileUtils.forceMkdir(out);
            }
            MessageUtils.globalLoading(id, "上传成功，开始解压缩...");
            //解压ZIP压缩文件
            ZipUtils.unZip(zipFie, out);
            File[] dirs = out.listFiles();
            //必须保证解压后仅有一个文件夹
            if (null == dirs || 1 != dirs.length || !dirs[0].isDirectory()) {
                MessageUtils.info("压缩文件中应当仅有一个文件夹！");
                return;
            }
            //解压后的文件夹
            File dir = dirs[0];
            //文件夹的名字
            final String name = dir.getName();
            //将要移动到的工作空间目录
            File dest = FileUtils.getFile(SettingUtils.getWorkspace(), userDir, name);
            boolean isExist = dest.exists();
            if (isExist) {
                String sid = SettingUtils.createSid(dest.getAbsolutePath());
                if (AgentManager.getInstance().isOnline(sid)) {
                    MessageUtils.info(name + " 正在运行，请先停止再导入！");
                    return;
                }
                MessageUtils.globalLoading(id, name + " 已存在，正在清除原目录...");
                //先删除
                FileUtils.deleteDirectory(dest);
            }
            //移动到工作空间目录
            MessageUtils.globalLoading(id, name + " 解压完成，开始拷贝...");
            FileUtils.copyDirectory(dir, dest);
            MessageUtils.globalLoading(id, name + " 推送完成！");
            //通知前端刷新列表
            if (isExist) {
                MessageUtils.info(name + " 更新成功！");
            } else {
                MessageUtils.globalEvent(FrontEndNotifyEventType.WORKSPACE_CHANGE);
                MessageUtils.info("推送成功，新增服务 " + name);
            }
        } catch (Exception e) {
            MessageUtils.error("推送失败！" + e.getMessage());
        } finally {
            //最终清理临时目录
            try {
                FileUtils.forceDelete(tempDir);
            } catch (Exception e) {
                //ignore
            }
            MessageUtils.globalLoading(id, StringUtils.EMPTY);
        }
    }
}
