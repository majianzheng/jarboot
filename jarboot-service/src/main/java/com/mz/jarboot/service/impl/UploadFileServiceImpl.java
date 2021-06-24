package com.mz.jarboot.service.impl;

import com.mz.jarboot.common.MzException;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.service.UploadFileService;
import com.mz.jarboot.utils.PropertyFileUtils;
import com.mz.jarboot.utils.SettingUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

@Service
public class UploadFileServiceImpl implements UploadFileService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String UPLOAD_LOCK_FILE = "upload.lock";
    private static final String BEGIN_TIME_KEY = "begin-time";
    private String tempDir = System.getProperty(CommonConst.JARBOOT_HOME) + File.separator + "tempDir";

    private File getTempCacheDir(String server) {
        String path = tempDir + File.separator + server;
        return new File(path);
    }

    private void cleanTempCacheDir(File dir) {
        File[] allFiles = dir.listFiles(file -> !UPLOAD_LOCK_FILE.equals(file.getName()));
        if (allFiles.length <= 0) {
            return;
        }
        for (File file : allFiles) {
            try {
                FileUtils.forceDelete(file);
            } catch (IOException e) {
                throw new MzException("清空文件夹失败！", e);
            }
        }
    }

    @Override
    public synchronized void beginUploadServerFile(String server) {
        File dir = getTempCacheDir(server);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new MzException("创建临时缓存目录失败！" + server);
        }
        File lockFile = new File(dir, UPLOAD_LOCK_FILE);
        Properties properties = PropertyFileUtils.getProperties(lockFile);
        String beginTime = properties.getProperty(BEGIN_TIME_KEY);
        if (StringUtils.isEmpty(beginTime)) {
            //清空文件
            cleanTempCacheDir(dir);
            properties.put(BEGIN_TIME_KEY, "" + System.currentTimeMillis());
            PropertyFileUtils.storeProperties(lockFile, properties);
        } else {
            throw new MzException("已经有其他客户端在上传！");
        }
    }

    @Override
    public void submitUploadFileInCache(String server) {
        if (StringUtils.isEmpty(server)) {
            throw new MzException("服务名为空！");
        }
        File dir = getTempCacheDir(server);
        String destPath = SettingUtils.getServerPath(server);
        File dest = new File(destPath);
        //开始复制前要不要先备份，以便失败后还原？文件量、体积巨大如何处理？为了性能先不做考虑
        try {
            //先复制jar文件
            Collection<File> jarFiles = FileUtils.listFiles(dir, new String[]{"jar"}, false);
            for (File jar : jarFiles) {
                FileUtils.copyFileToDirectory(jar, dest, true);
            }
            //zip文件处理

        } catch (Exception e) {
            //还原目录?万一体积巨大怎么处理
            throw new MzException(e.getMessage(), e);
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
                throw new MzException("删除失败！" + file, e);
            }
        }
    }

    @Override
    public void clearUploadFileInCache(String server) {
        File dir = getTempCacheDir(server);
        if (dir.exists() && dir.isDirectory()) {
            try {
                FileUtils.deleteDirectory(dir);
            } catch (IOException e) {
                throw new MzException("删除失败！" + server, e);
            }
        }
    }

    @Override
    public void uploadJarFiles(MultipartFile file, String server) {
        logger.info("type:{}, name:{}, size:{}, oriName:{}, server:{}", file.getContentType(),
                file.getName(), file.getSize(), file.getOriginalFilename(), server);
        File dir = getTempCacheDir(server);
        if (dir.exists() && dir.isDirectory()) {
            File f = new File(dir, file.getOriginalFilename());
            try {
                file.transferTo(f);
            } catch (IOException e) {
                throw new MzException("上传失败！" + file.getOriginalFilename(), e);
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
