package com.mz.jarboot.service.impl;

import com.mz.jarboot.common.MzException;
import com.mz.jarboot.common.NetworkUtils;
import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.dto.ServerRunningDTO;
import com.mz.jarboot.service.ArthasAdapterService;
import com.mz.jarboot.service.ServerMgrService;
import com.mz.jarboot.utils.SettingUtils;
import com.mz.jarboot.utils.TaskUtils;
import com.mz.jarboot.utils.VMUtils;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.List;
import java.util.Map;

@Service
public class ArthasAdapterServiceImpl implements ArthasAdapterService {
    private static final String ARTHAS_BOOT_JAR = "arthas-boot.jar";
    private static final int ARTHAS_DEFAULT_PORT = 3658;

    @Autowired
    private ServerMgrService serverMgrService;
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
        //检查有没有进程占用了3658端口
        String current = getCurrentRunning();
        if (StringUtils.isNotEmpty(current)) {
            throw new MzException(ResultCodeConst.NOT_EXIST, String.format("服务%s正在调试中", current));
        }
        int pid = TaskUtils.getServerPid(server);
        if (CommonConst.INVALID_PID == pid) {
            throw new MzException(ResultCodeConst.NOT_EXIST, "服务进程尚未启动");
        }
        String arthasBoot = SettingUtils.getGlobalSetting().getArthasHome() + File.separator + ARTHAS_BOOT_JAR;

        String cmd = String.format("java -jar \"%s\" %d", arthasBoot, pid);
        TaskUtils.startTask(cmd, outLine -> WebSocketManager.getInstance().noticeInfo(outLine));
    }

    @Override
    public synchronized String getCurrentRunning() {
        int pid = NetworkUtils.findProcessByListenPort(ARTHAS_DEFAULT_PORT);
        if (CommonConst.INVALID_PID == pid) {
            return  "";
        } else {
            //有正在调试的进程
            Map<Integer, String> vmList = VMUtils.getInstance().listVM();
            String name = vmList.get(pid);
            if (StringUtils.isEmpty(name)) {
                throw new MzException(ResultCodeConst.INTERNAL_ERROR, "有其他进程占用了端口");
            }
            List<ServerRunningDTO> serverList = serverMgrService.getServerList();
            for (ServerRunningDTO serverDetail : serverList) {
                String s = TaskUtils.getJarWithServerName(serverDetail.getName());
                if (StringUtils.contains(name, s)) {
                    //得到正在调试的服务
                    return serverDetail.getName();
                }
            }
            return name;
        }
    }
}
