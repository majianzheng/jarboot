package com.mz.jarboot.service.impl;

import com.mz.jarboot.common.MzException;
import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.service.ArthasAdapterService;
import com.mz.jarboot.utils.SettingUtils;
import com.mz.jarboot.utils.TaskUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class ArthasAdapterServiceImpl implements ArthasAdapterService {
    private static final String ARTHAS_BOOT_JAR = "arthas-boot.jar";
    private Process process = null;
    private String currentServer = "";
    @Override
    public boolean checkArthasInstalled() {
        //先获取是否有ARTHAS_HOME的环境变量
        String arthasHome = System.getenv("ARTHAS_HOME");
        if (StringUtils.isEmpty(arthasHome)) {
            arthasHome = SettingUtils.getGlobalSetting().getArthasHome();
        }
        if (StringUtils.isEmpty(arthasHome)) {
            return false;
        }
        File file = new File(arthasHome + File.separator + ARTHAS_BOOT_JAR);
        return (file.isFile() && file.exists());
    }

    @Override
    public synchronized void attachToServer(String server) {
        if (null != process) {
            if (process.isAlive()) {
                throw new MzException(ResultCodeConst.VALIDATE_FAILED, "当前正在运行Arthas");
            }
            process = null;
        }
        int pid = TaskUtils.getServerPid(server);
        if (CommonConst.INVALID_PID == pid) {
            throw new MzException(ResultCodeConst.NOT_EXIST, "服务进程尚未启动");
        }
        String arthasBoot = SettingUtils.getGlobalSetting().getArthasHome() + File.separator + ARTHAS_BOOT_JAR;

        String cmd = String.format("java -jar %s %d", arthasBoot, pid);
        process = TaskUtils.startTask(cmd, outLine -> {

        });
        this.currentServer = server;
    }

    @Override
    public synchronized String getCurrentRunning() {
        return this.currentServer;
    }

    @Override
    public synchronized void stopCurrentArthasInstance() {
        if (null != process) {
            process.destroy();
            currentServer = "";
        }
    }
}
