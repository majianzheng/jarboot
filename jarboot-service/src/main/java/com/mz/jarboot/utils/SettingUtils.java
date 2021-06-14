package com.mz.jarboot.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.common.MzException;
import com.mz.jarboot.dto.GlobalSettingDTO;
import com.mz.jarboot.event.NoticeEnum;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.util.*;

public class SettingUtils {
    private static final Logger logger = LoggerFactory.getLogger(SettingUtils.class);
    private static GlobalSettingDTO globalSetting = null;
    private static final String BOOT_INI = "boot.ini";
    private static final String GLOBAL_SETTING_FILE = "global-setting.conf";
    private static final String DEFAULT_SERVICES_ROOT_NAME = "services";
    private static String agentJar;
    static {
        //jarboot-agent.jar的路径获取
        initAgentJarPath();
        //初始化路径配置，先查找
        initGlobalSetting();
        //进行默认配置
        initDefaultServicesRootPath();
    }

    private static void initAgentJarPath() {
        CodeSource codeSource = SettingUtils.class.getProtectionDomain().getCodeSource();
        try {
            File curJar = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
            File jarFile = new File(curJar.getParentFile(), CommonConst.AGENT_JAR_NAME);

            //先尝试从当前路径下获取jar的位置
            if (jarFile.exists()) {
                agentJar = jarFile.getPath();
            } else {
                agentJar = System.getProperty(CommonConst.WORKSPACE_HOME) + File.separator + CommonConst.AGENT_JAR_NAME;
                jarFile = new File(agentJar);
                if (!jarFile.exists()) {
                    throw new MzException(ResultCodeConst.NOT_EXIST, "从用户路径下未发现" + agentJar);
                }
            }
        } catch (Exception e) {
            //查找jarboot-agent.jar失败
            logger.error("Can not find jarboot-agent.jar.", e);
            System.exit(-1);
        }
    }
    private static void initGlobalSetting() {
        String conf = System.getProperty(CommonConst.JARBOOT_HOME) + File.separator + GLOBAL_SETTING_FILE;
        File file = FileUtils.getFile(conf);
        if (file.isDirectory()) {
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                logger.error("存在与配置文件相同的文件夹名称，删除失败！");
                return;
            }
        }

        if (!file.exists()) {
            //不存在配置文件
            return;
        }
        try {
            String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            globalSetting = JSON.parseObject(content, GlobalSettingDTO.class);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
    private static void initDefaultServicesRootPath() {
        if (null == globalSetting) {
            globalSetting = new GlobalSettingDTO();
        }
        String servicesPath = globalSetting.getServicesPath();
        if (StringUtils.isNotEmpty(servicesPath)) {
            File dir = new File(servicesPath);
            if (dir.exists() && dir.isDirectory()) {
                return;
            }
            logger.warn("配置的services({})路径不存在，将使用默认的约定路径。", servicesPath);
        }
        servicesPath = System.getProperty(CommonConst.JARBOOT_HOME) + File.separator + DEFAULT_SERVICES_ROOT_NAME;
        globalSetting.setServicesPath(servicesPath);
    }

    public static GlobalSettingDTO getGlobalSetting() {
        return globalSetting;
    }

    public static void updateGlobalSetting(GlobalSettingDTO setting) {
        String servicesPath = setting.getServicesPath();
        if (StringUtils.isNotEmpty(servicesPath)) {
            File dir = new File(servicesPath);
            if (!dir.isDirectory() || !dir.exists()) {
                throw new MzException(ResultCodeConst.NOT_EXIST, String.format("配置的路径%s不存在！", servicesPath));
            }
        }

        String arthasHome = setting.getArthasHome();
        if (StringUtils.isNotEmpty(arthasHome)) {
            File dir = new File(arthasHome);
            if (!dir.isDirectory() || !dir.exists()) {
                throw new MzException(ResultCodeConst.NOT_EXIST, String.format("配置的路径%s不存在！", arthasHome));
            }
        }

        String conf = System.getProperty(CommonConst.JARBOOT_HOME) + File.separator + GLOBAL_SETTING_FILE;
        File file = FileUtils.getFile(conf);
        try {
            if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
            }
            FileUtils.writeStringToFile(file, JSON.toJSONString(setting), StandardCharsets.UTF_8);
            //再更新到内存
            globalSetting = setting;
        } catch (Exception e) {
            throw new MzException(ResultCodeConst.INTERNAL_ERROR, "更新全局配置文件失败！", e);
        }
    }


    public static String getServicesPath() {
        return globalSetting.getServicesPath();
    }

    public static String getAgentStartOption(String server) {
        return "-javaagent:" + agentJar + "=" + getAgentArgs(server);
    }

    public static String getAgentJar() {
        return agentJar;
    }

    public static String getAgentArgs(String server) {
        JSONObject json = new JSONObject();
        json.put("host", "127.0.0.1:9899");
        json.put("server", server);
        byte[] bytes = Base64.getEncoder().encode(json.toJSONString().getBytes());
        return new String(bytes);
    }

    public static String getDefaultJvmArg() {
        return globalSetting.getDefaultJvmArg();
    }

    /**
     * 获取服务的jar包路径
     * @param server 服务名
     * @return jar包路径
     */
    public static String getJarPath(String server) {
        File dir = new File(getServerPath(server));
        if (!dir.isDirectory() || !dir.exists()) {
            logger.error("未找到{}服务的jar包路径{}", server, dir.getPath());
            WebSocketManager.getInstance().notice("未找到服务" + server + "的可执行jar包路径", NoticeEnum.WARN);
        }
        String[] extensions = {"jar"};
        Collection<File> jarList = FileUtils.listFiles(dir, extensions, false);
        if (org.apache.commons.collections.CollectionUtils.isEmpty(jarList)) {
            logger.error("在{}未找到{}服务的jar包", server, dir.getPath());
            WebSocketManager.getInstance().notice("未找到服务" + server + "的可执行jar包", NoticeEnum.ERROR);
        }
        if (jarList.size() > 1) {
            WebSocketManager.getInstance()
                    .notice("在服务目录找到了多个jar包！可能会导致服务不可用，请先清理该目录！留下一个可用的jar包文件！"
                            , NoticeEnum.WARN);
        }
        if (jarList.iterator().hasNext()) {
            File jarFile = jarList.iterator().next();
            return jarFile.getPath();
        }
        return "";
    }

    public static String getServerPath(String server) {
        return getServicesPath() + File.separator + server;
    }

    public static String getServerSettingFilePath(String server) {
        StringBuilder builder = new StringBuilder();
        builder.append(getServicesPath()).append(File.separator).append(server).append(File.separator).append(BOOT_INI);
        return builder.toString();
    }

    private SettingUtils() {

    }
}
