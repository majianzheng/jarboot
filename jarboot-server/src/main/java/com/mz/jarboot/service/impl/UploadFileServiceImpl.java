package com.mz.jarboot.service.impl;

import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.pojo.ServerSetting;
import com.mz.jarboot.event.NoticeEnum;
import com.mz.jarboot.service.UploadFileService;
import com.mz.jarboot.utils.PropertyFileUtils;
import com.mz.jarboot.utils.SettingUtils;
import com.mz.jarboot.utils.TaskUtils;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 为了防止浏览器端的各种异常场景下后台缓冲区不能及时清理的情况，现设计如下机制
 * 将文件的上传分为3个阶段：
 * 1、预备阶段，原子操作同时同一个服务只能有一个客户端，确定要上传的服务名，确定后不可修改，创建缓冲区目录，开启心跳监测；
 * 2、开始上传阶段，将文件一个个上传到创建的缓冲区中，此间前端必须每隔5秒钟向后端探测一次心跳；
 * 3、提交 & 取消，停止心跳监测，将第二阶段上传的一批文件复制到服务的目录下，并清理缓冲区目录。
 * 停止心跳探测后，至少会过两个周期才会停止心跳监控的线程
 * @author majianzheng
 */
@Service
public class UploadFileServiceImpl implements UploadFileService {
    private static final long EXPIRED_TIME = 20000;
    private String tempDir = System.getProperty(CommonConst.JARBOOT_HOME) + File.separator + "tempDir";
    private ConcurrentHashMap<String, Long> uploadHeartbeat = new ConcurrentHashMap<>();
    /** 是否启动了心跳监测 */
    private volatile boolean started = false;

    private File getTempCacheDir(String server) {
        return FileUtils.getFile(tempDir, server);
    }

    private void cleanTempCacheDir(File dir) {
        File[] allFiles = dir.listFiles();
        if (null != allFiles && allFiles.length > 0) {
            for (File file : allFiles) {
                try {
                    FileUtils.forceDelete(file);
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

    /**
     * 发起beginUploadServerFile后，将开启监控，如果前端没有定期发起心跳则判定过期，清理缓冲区文件
     */
    private void startMonitor() {
        if (started) {
            return;
        }
        TaskUtils.getTaskExecutor().execute(() -> {
            //前端每隔5秒探测一次，后端每隔15秒检查一次，如果发现时间戳相差大于15则说明至少3个周期没有心跳了，判定过期
            for (;;) {
                try {
                    TimeUnit.MILLISECONDS.sleep(EXPIRED_TIME);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (uploadHeartbeat.isEmpty()) {
                    //当没有任何服务在上传时，将线程归还线程池
                    started = false;
                    break;
                }
                long currentTime = System.currentTimeMillis();
                Stream<Map.Entry<String, Long>> entries = uploadHeartbeat.entrySet().stream();
                List<String> waitDelete = entries.filter(entry -> (currentTime - entry.getValue()) > EXPIRED_TIME)
                        .map(Map.Entry::getKey).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(waitDelete)) {
                    // 清理失去心跳的key
                    waitDelete.forEach(this::clearUploadFileInCache);
                }
            }
        });
    }

    @Override
    public synchronized void beginUploadServerFile(String server) {
        if (uploadHeartbeat.containsKey(server)) {
            throw new JarbootException("已经有其他客户端在上传！");
        }
        File dir = getTempCacheDir(server);
        if (dir.exists()) {
            cleanTempCacheDir(dir);
        }
        if (!dir.mkdirs()) {
            throw new JarbootException("创建临时缓存目录失败！" + server);
        }
        uploadHeartbeat.put(server, System.currentTimeMillis());
        startMonitor();
    }

    @Override
    public void uploadServerHeartbeat(String server) {
        //如果存在则更新时间戳，不存在则忽略，保证原子操作
        uploadHeartbeat.computeIfPresent(server, (k, v) -> System.currentTimeMillis());
        if (!uploadHeartbeat.containsKey(server)) {
            // 通知前端停止继续探测
            throw new JarbootException("心跳已经失效！");
        }
    }

    @Override
    public synchronized void submitUploadFileInCache(String server) {
        if (StringUtils.isEmpty(server)) {
            throw new JarbootException("服务名为空！");
        }
        File dir = getTempCacheDir(server);
        String destPath = SettingUtils.getServerPath(server);
        File dest = FileUtils.getFile(destPath);
        //开始复制前要不要先备份，以便失败后还原？文件量、体积巨大如何处理？为了性能先不做考虑
        try {
            //先复制jar文件
            Collection<File> jarFiles = FileUtils.listFiles(dir, CommonConst.JAR_FILE_EXT, false);
            for (File jar : jarFiles) {
                FileUtils.copyFileToDirectory(jar, dest, true);
            }
            //zip文件处理

            //检测多个jar文件时有没有配置启动的jar文件
            ServerSetting setting = PropertyFileUtils.getServerSetting(server);
            if (StringUtils.isEmpty(setting.getJar())) {
                boolean bo = FileUtils.listFiles(dest, CommonConst.JAR_FILE_EXT, false).size() > 1;
                if (bo) {
                    String msg = String.format("在服务%s目录找到了多个jar文件，请设置启动的jar文件！", server);
                    WebSocketManager.getInstance().notice(msg, NoticeEnum.WARN);
                }
            }
        } catch (Exception e) {
            //还原目录?万一体积巨大怎么处理
            throw new JarbootException(e.getMessage(), e);
        } finally {
            //清理缓存文件
            clearUploadFileInCache(server);
        }
    }

    @Override
    public void deleteUploadFileInCache(String server, String file) {
        File dir = getTempCacheDir(server);
        File[] find = dir.listFiles(f -> StringUtils.equals(file, f.getName()));
        if (null != find && find.length > 0) {
            try {
                FileUtils.forceDelete(find[0]);
            } catch (IOException e) {
                throw new JarbootException("删除失败！" + file, e);
            }
        }
    }

    @Override
    public synchronized void clearUploadFileInCache(String server) {
        uploadHeartbeat.remove(server);
        File dir = getTempCacheDir(server);
        if (dir.exists() && dir.isDirectory()) {
            try {
                FileUtils.deleteDirectory(dir);
            } catch (Exception e) {
                //ignore
            }
        }
    }

    @Override
    public void uploadJarFiles(MultipartFile file, String server) {
        File dir = getTempCacheDir(server);
        if (dir.exists() && dir.isDirectory()) {
            String name = file.getOriginalFilename();
            if (StringUtils.isEmpty(name)) {
                throw new JarbootException("文件原始名字不可为空！");
            }
            File f = FileUtils.getFile(dir, name);
            try {
                file.transferTo(f);
            } catch (IOException e) {
                throw new JarbootException("上传失败！" + file.getOriginalFilename(), e);
            }
        }
    }

    @PostConstruct
    public void init() {
        //清理tempDir目录
        File dir = new File(tempDir);
        if (dir.exists()) {
            try {
                FileUtils.deleteDirectory(dir);
            } catch (Exception e) {
                //ignore
            }
        }
    }
}
