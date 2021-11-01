package com.mz.jarboot.utils;

import com.mz.jarboot.common.*;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.constant.SettingPropConst;
import com.mz.jarboot.api.pojo.GlobalSetting;
import com.mz.jarboot.api.pojo.ServerSetting;
import com.mz.jarboot.event.ApplicationContextUtils;
import com.mz.jarboot.event.NoticeEnum;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author majianzheng
 */
public class SettingUtils {
    private static final Logger logger = LoggerFactory.getLogger(SettingUtils.class);
    private static final GlobalSetting GLOBAL_SETTING = new GlobalSetting();
    private static final String BOOT_PROPERTIES = "boot.properties";
    private static final String ROOT_DIR_KEY = "jarboot.services.workspace";
    private static final String DEFAULT_VM_OPTS_KEY = "jarboot.services.default-vm-options";
    private static final String DEFAULT_WORKSPACE;
    private static final String ENABLE_AUTO_START_KEY = "jarboot.services.enable-auto-start-after-start";
    private static final String JARBOOT_CONF;
    private static final String BIN_DIR;
    private static final String LOG_DIR;

    private static String agentJar;
    static {
        String home = System.getProperty(CommonConst.JARBOOT_HOME);
        JARBOOT_CONF = home + File.separator + "conf" + File.separator + "jarboot.properties";
        BIN_DIR = home + File.separator + "bin";
        LOG_DIR = home + File.separator + "logs";
        //jarboot-agent.jar的路径获取
        initAgentJarPath();
        //初始化路径配置，先查找
        initGlobalSetting();
        //初始化默认目录及配置路径
        DEFAULT_WORKSPACE = System.getProperty(CommonConst.JARBOOT_HOME) + File.separator + CommonConst.SERVICES;
    }

    private static void initAgentJarPath() {
        File jarFile = new File(BIN_DIR, CommonConst.AGENT_JAR_NAME);
        //先尝试从当前路径下获取jar的位置
        if (jarFile.exists()) {
            agentJar = jarFile.getPath();
        } else {
            logger.error("文件不存在 {}",  agentJar);
            System.exit(-1);
        }
    }
    private static void initGlobalSetting() {
        File conf = new File(JARBOOT_CONF);
        Properties properties = (conf.exists() && conf.isFile() && conf.canRead()) ?
                PropertyFileUtils.getProperties(conf) : new Properties();
        GLOBAL_SETTING.setWorkspace(properties.getProperty(ROOT_DIR_KEY, StringUtils.EMPTY));
        GLOBAL_SETTING.setDefaultVmOptions(properties.getProperty(DEFAULT_VM_OPTS_KEY, StringUtils.EMPTY).trim());
        String s = properties.getProperty(ENABLE_AUTO_START_KEY, SettingPropConst.VALUE_FALSE);
        boolean servicesAutoStart = StringUtils.equalsIgnoreCase(SettingPropConst.VALUE_TRUE, s);
        GLOBAL_SETTING.setServicesAutoStart(servicesAutoStart);
    }

    public static GlobalSetting getGlobalSetting() {
        return GLOBAL_SETTING;
    }

    public static void updateGlobalSetting(GlobalSetting setting) {
        String workspace = setting.getWorkspace();
        if (StringUtils.isNotEmpty(workspace)) {
            File dir = new File(workspace);
            if (!dir.isDirectory() || !dir.exists()) {
                throw new JarbootException(ResultCodeConst.NOT_EXIST, String.format("配置的路径%s不存在！", workspace));
            }
        }

        File file = FileUtils.getFile(JARBOOT_CONF);
        try {
            HashMap<String, String> props = new HashMap<>(4);
            if (null == setting.getDefaultVmOptions()) {
                props.put(DEFAULT_VM_OPTS_KEY, StringUtils.EMPTY);
            } else {
                props.put(DEFAULT_VM_OPTS_KEY, setting.getDefaultVmOptions());
            }
            if (OSUtils.isWindows()) {
                props.put(ROOT_DIR_KEY, workspace.replace('\\', '/'));
            } else {
                props.put(ROOT_DIR_KEY, workspace);
            }

            props.put(ENABLE_AUTO_START_KEY, String.valueOf(setting.getServicesAutoStart()));
            PropertyFileUtils.writeProperty(file, props);
            //再更新到内存
            if (null == setting.getDefaultVmOptions()) {
                GLOBAL_SETTING.setDefaultVmOptions(StringUtils.EMPTY);
            } else {
                GLOBAL_SETTING.setDefaultVmOptions(setting.getDefaultVmOptions().trim());
            }
            GLOBAL_SETTING.setWorkspace(workspace);
            GLOBAL_SETTING.setServicesAutoStart(setting.getServicesAutoStart());
        } catch (Exception e) {
            throw new JarbootException(ResultCodeConst.INTERNAL_ERROR, "更新全局配置文件失败！", e);
        }
    }


    public static String getWorkspace() {
        String path = GLOBAL_SETTING.getWorkspace();
        if (StringUtils.isBlank(path)) {
            path = DEFAULT_WORKSPACE;
        }
        return path;
    }

    public static String getLogDir() {
        return LOG_DIR;
    }

    public static String getAgentStartOption(String server, String sid) {
        return "-javaagent:" + agentJar + "=" + getAgentArgs(server, sid);
    }

    public static String getAgentJar() {
        return agentJar;
    }

    public static String getAgentArgs(String server, String sid) {
        String port = ApplicationContextUtils.getEnv(CommonConst.PORT_KEY, CommonConst.DEFAULT_PORT);
        StringBuilder sb = new StringBuilder();
        sb
                .append("127.0.0.1:")
                .append(port)
                .append(CommandConst.PROTOCOL_SPLIT)
                .append(server)
                .append(CommandConst.PROTOCOL_SPLIT)
                .append(sid);
        byte[] bytes = Base64.getEncoder().encode(sb.toString().getBytes());
        return new String(bytes);
    }

    public static String getDefaultJvmArg() {
        String defaultVmOptions = GLOBAL_SETTING.getDefaultVmOptions();
        return null == defaultVmOptions ? StringUtils.EMPTY : defaultVmOptions;
    }

    /**
     * 获取服务的jar包路径
     * @param setting 服务配置
     * @return jar包路径
     */
    public static String getJarPath(ServerSetting setting) {
        String server = setting.getServer();
        File dir = FileUtils.getFile(setting.getPath());
        if (!dir.isDirectory() || !dir.exists()) {
            logger.error("未找到{}服务的jar包路径{}", server, dir.getPath());
            WebSocketManager.getInstance().notice("未找到服务" + server + "的可执行jar包路径", NoticeEnum.WARN);
        }
        if (StringUtils.isNotEmpty(setting.getJar())) {
            //配置了jar文件，判定是否绝对路径
            Path path = Paths.get(setting.getJar());
            File jar = path.isAbsolute() ? path.toFile() : FileUtils.getFile(dir, setting.getJar());
            if (jar.exists() && jar.isFile()) {
                return jar.getPath();
            } else {
                WebSocketManager.getInstance()
                        .notice("设置启动的jar文件不存在，请重新设置！", NoticeEnum.WARN);
            }
        }

        //未指定时
        Collection<File> jarList = FileUtils.listFiles(dir, CommonConst.JAR_FILE_EXT, false);
        if (org.apache.commons.collections.CollectionUtils.isEmpty(jarList)) {
            logger.error("在{}未找到{}服务的jar包", server, dir.getPath());
            WebSocketManager.getInstance().notice("未找到服务" + server + "的可执行jar包", NoticeEnum.ERROR);
            return StringUtils.EMPTY;
        }
        if (jarList.size() > 1) {
            String msg = String.format("在服务%s目录找到了多个jar文件，请设置启动的jar文件！", server);
            WebSocketManager.getInstance().notice(msg, NoticeEnum.WARN);
            return StringUtils.EMPTY;
        }
        if (jarList.iterator().hasNext()) {
            File jarFile = jarList.iterator().next();
            return jarFile.getPath();
        }
        return StringUtils.EMPTY;
    }

    public static String getServerPath(String server) {
        return getWorkspace() + File.separator + server;
    }

    public static File getServerSettingFile(String path) {
        return FileUtils.getFile(path, BOOT_PROPERTIES);
    }

    public static String getJvm(String serverPath, String file) {
        if (StringUtils.isBlank(file)) {
            file = SettingPropConst.DEFAULT_VM_FILE;
        }
        Path path = Paths.get(file);
        if (!path.isAbsolute()) {
            path = Paths.get(serverPath, file);
        }
        File f = path.toFile();
        StringBuilder sb = new StringBuilder();
        if (f.exists()) {
            List<String> lines;
            try {
                lines = FileUtils.readLines(f, StandardCharsets.UTF_8);
            } catch (IOException e) {
                WebSocketManager.getInstance().notice(e.getMessage(), NoticeEnum.WARN);
                throw new JarbootException("Read file error.", e);
            }
            lines.stream()
                    //去除首尾空格
                    .map(StringUtils::trim)
                    //以#开头的视为注释
                    .filter(line -> SettingPropConst.COMMENT_PREFIX != line.charAt(0))
                    .forEach(line -> sb.append(line).append(StringUtils.SPACE));
        }
        String vm = sb.toString();
        if (StringUtils.isBlank(vm)) {
            vm = SettingUtils.getDefaultJvmArg();
            if (!vm.isEmpty() && !vm.endsWith(StringUtils.SPACE)) {
                vm += StringUtils.SPACE;
            }
        }
        return vm;
    }

    public static String createSid(String serverPath) {
        return String.format("%x", serverPath.hashCode());
    }

    private SettingUtils() {

    }
}
