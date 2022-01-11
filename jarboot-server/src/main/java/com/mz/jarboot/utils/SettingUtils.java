package com.mz.jarboot.utils;

import com.mz.jarboot.common.*;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.constant.SettingPropConst;
import com.mz.jarboot.api.pojo.GlobalSetting;
import com.mz.jarboot.api.pojo.ServerSetting;
import com.mz.jarboot.common.utils.OSUtils;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.event.ApplicationContextUtils;
import com.mz.jarboot.event.NoticeEnum;
import com.mz.jarboot.service.TaskWatchService;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

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

    /** 系统配置缓存 */
    private static final GlobalSetting GLOBAL_SETTING = new GlobalSetting();
    /** Jarboot配置文件名字 */
    private static final String BOOT_PROPERTIES = "boot.properties";
    /** 工作空间属性key */
    private static final String ROOT_DIR_KEY = "jarboot.services.workspace";
    /** 默认VM参数属性key */
    private static final String DEFAULT_VM_OPTS_KEY = "jarboot.services.default-vm-options";
    /** 默认的工作空间路径 */
    private static final String DEFAULT_WORKSPACE;
    /** Jarboot启动后自动启动其管理的服务的属性配置key */
    private static final String ENABLE_AUTO_START_KEY = "jarboot.services.enable-auto-start-after-start";
    /** Jarboot配置文件路径 */
    private static final String JARBOOT_CONF;
    /** Jarboot的bin文件夹路径 */
    private static final String BIN_DIR;
    /** Jarboot的日志路径 */
    private static final String LOG_DIR;
    /** jarboot-agent.jar文件的路径 */
    private static String agentJar;
    /** file encoding选项 */
    private static final String FILE_ENCODING_OPTION = "-Dfile.encoding=";
    /** 本地地址 */
    private static String localHost = null;
    /** 受信任的远程服务器 */
    private static final String TRUSTED_HOSTS_FILE;
    private static HashSet<String> trustedHosts = new HashSet<>(16);

    static {
        final String home = System.getProperty(CommonConst.JARBOOT_HOME);
        final String conf = home + File.separator + "conf" + File.separator;
        JARBOOT_CONF = conf + "jarboot.properties";
        BIN_DIR = home + File.separator + "bin";
        LOG_DIR = home + File.separator + "logs";
        //jarboot-agent.jar的路径获取
        initAgentJarPath();
        //初始化路径配置，先查找
        initGlobalSetting();
        //初始化受信任服务器列表
        TRUSTED_HOSTS_FILE = conf + "trusted-hosts.conf";
        initTrustedHosts();
        //初始化默认目录及配置路径
        DEFAULT_WORKSPACE = System.getProperty(CommonConst.JARBOOT_HOME) + File.separator + CommonConst.SERVICES;
    }

    /**
     * 初始化Agent路径
     */
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

    /**
     * 初始化系统配置
     */
    private static void initGlobalSetting() {
        File conf = new File(JARBOOT_CONF);
        Properties properties = (conf.exists() && conf.isFile() && conf.canRead()) ?
                PropertyFileUtils.getProperties(conf) : new Properties();
        GLOBAL_SETTING.setWorkspace(properties.getProperty(ROOT_DIR_KEY, StringUtils.EMPTY));
        GLOBAL_SETTING.setDefaultVmOptions(properties.getProperty(DEFAULT_VM_OPTS_KEY, StringUtils.EMPTY).trim());
        String s = properties.getProperty(ENABLE_AUTO_START_KEY, SettingPropConst.VALUE_FALSE);
        boolean servicesAutoStart = SettingPropConst.VALUE_TRUE.equalsIgnoreCase(s);
        GLOBAL_SETTING.setServicesAutoStart(servicesAutoStart);
    }

    private static void initTrustedHosts() {
        File file = FileUtils.getFile(TRUSTED_HOSTS_FILE);
        if (!file.exists()) {
            return;
        }
        if (!file.isFile()) {
            FileUtils.deleteQuietly(file);
            return;
        }
        try {
            List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
            lines.forEach(line -> trustedHosts.add(line.trim()));
        } catch (Exception e) {
            //ignore
        }
    }

    /**
     * 获取系统配置
     * @return 系统配置
     */
    public static GlobalSetting getGlobalSetting() {
        return GLOBAL_SETTING;
    }

    /**
     * 更新系统配置
     * @param setting 配置
     */
    public static synchronized void updateGlobalSetting(GlobalSetting setting) {
        String workspace = setting.getWorkspace();
        if (StringUtils.isNotEmpty(workspace)) {
            File dir = new File(workspace);
            if (!dir.isDirectory() || !dir.exists()) {
                throw new JarbootException(ResultCodeConst.NOT_EXIST, String.format("配置的路径%s不存在！", workspace));
            }
        }

        File file = FileUtils.getFile(JARBOOT_CONF);
        boolean workspaceChanged = false;
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
            if (!java.util.Objects.equals(GLOBAL_SETTING.getWorkspace(), workspace)) {
                workspaceChanged = true;
            }
            GLOBAL_SETTING.setWorkspace(workspace);
            GLOBAL_SETTING.setServicesAutoStart(setting.getServicesAutoStart());
        } catch (Exception e) {
            throw new JarbootException(ResultCodeConst.INTERNAL_ERROR, "更新全局配置文件失败！", e);
        } finally {
            if (workspaceChanged) {
                //工作空间修改，改变工作空间路径监控的目录
                ApplicationContextUtils.getContext().getBean(TaskWatchService.class).changeWorkspace(workspace);
            }
        }
    }


    /**
     * 获取工作空间
     * @return 工作空间
     */
    public static String getWorkspace() {
        String path = GLOBAL_SETTING.getWorkspace();
        if (StringUtils.isBlank(path)) {
            path = DEFAULT_WORKSPACE;
        }
        return path;
    }

    /**
     * 获取日志目录
     * @return 日志目录
     */
    public static String getLogDir() {
        return LOG_DIR;
    }

    /**
     * 获取agent的Attach参数
     * @param server 服务名
     * @param sid 服务唯一id
     * @return 参数
     */
    public static String getAgentStartOption(String server, String sid) {
        return "-javaagent:" + agentJar + "=" + getAgentArgs(server, sid);
    }

    public static String getAgentJar() {
        return agentJar;
    }

    private static String getAgentArgs(String server, String sid) {
        String port = ApplicationContextUtils.getEnv(CommonConst.PORT_KEY, CommonConst.DEFAULT_PORT);
        StringBuilder sb = new StringBuilder();
        sb
                .append(port)
                .append(StringUtils.CR)
                .append(server)
                .append(StringUtils.CR)
                .append(sid);
        byte[] bytes = Base64.getEncoder().encode(sb.toString().getBytes());
        return new String(bytes);
    }

    public static String getAttachArgs() {
        if (null == localHost) {
            String port = ApplicationContextUtils.getEnv(CommonConst.PORT_KEY, CommonConst.DEFAULT_PORT);
            localHost = String.format("127.0.0.1:%s", port);
        }
        return localHost;
    }

    /**
     * 获取默认的VM参数
     * @return VM配置
     */
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
        String server = setting.getName();
        File dir = FileUtils.getFile(setting.getPath());
        if (!dir.isDirectory() || !dir.exists()) {
            logger.error("未找到{}服务的jar包路径{}", server, dir.getPath());
            WebSocketManager.getInstance().notice("未找到服务" + server + "的可执行jar包路径", NoticeEnum.WARN);
        }

        Collection<File> jarList = FileUtils.listFiles(dir, CommonConst.JAR_FILE_EXT, false);
        if (CollectionUtils.isEmpty(jarList)) {
            logger.error("在{}未找到{}服务的jar包", server, dir.getPath());
            WebSocketManager.getInstance().notice("未找到服务" + server + "的可执行jar包", NoticeEnum.ERROR);
            return StringUtils.EMPTY;
        }
        if (jarList.size() > 1) {
            String msg = String.format("在服务%s目录找到了多个jar文件，请配置启动命令！", server);
            WebSocketManager.getInstance().notice(msg, NoticeEnum.WARN);
            return StringUtils.EMPTY;
        }
        if (jarList.iterator().hasNext()) {
            File jarFile = jarList.iterator().next();
            return jarFile.getPath();
        }
        return StringUtils.EMPTY;
    }

    /**
     * 获取服务工作路径
     * @param server 服务名
     * @return 路径
     */
    public static String getServerPath(String server) {
        return getWorkspace() + File.separator + server;
    }

    /**
     * 获取服务启动配置文件
     * @param path 路径
     * @return 配置文件
     */
    public static File getServerSettingFile(String path) {
        return FileUtils.getFile(path, BOOT_PROPERTIES);
    }

    /**
     * 获取服务VM选项配置
     * @param serverPath 服务路径
     * @param file 配置文件
     * @return VM选项
     */
    public static String getJvm(String serverPath, String file) {
        if (StringUtils.isBlank(file)) {
            file = SettingPropConst.DEFAULT_VM_FILE;
        }
        Path path = getPath(file);
        if (!path.isAbsolute()) {
            path = getPath(serverPath, file);
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
                    .map(String::trim)
                    //以#开头的视为注释
                    .filter(line -> SettingPropConst.COMMENT_PREFIX != line.charAt(0))
                    .forEach(line -> sb.append(line).append(StringUtils.SPACE));
        }
        String vm = sb.toString().trim();
        if (StringUtils.isBlank(vm)) {
            vm = SettingUtils.getDefaultJvmArg().trim();
        }
        if (!vm.contains(FILE_ENCODING_OPTION)) {
            vm += (StringUtils.SPACE + FILE_ENCODING_OPTION + StandardCharsets.UTF_8);
        }
        return vm.trim();
    }

    public static Path getPath(String file, String... more) {
        return Paths.get(file, more);
    }

    /**
     * 检查是否绝对路径
     * @param file 文件
     * @return 是否绝对路径
     */
    public static boolean isAbsolutePath(String file) {
        Path path = getPath(file);
        return path.isAbsolute();
    }

    /**
     * 根据路径生成sid
     * @param serverPath 服务路径
     * @return sid
     */
    public static String createSid(String serverPath) {
        return String.format("x%x", serverPath.hashCode());
    }

    public static boolean isTrustedHost(String host) {
        if (StringUtils.isBlank(host)) {
            return false;
        }
        return trustedHosts.contains(host);
    }

    public static void addTrustedHost(String host) throws IOException {
        if (StringUtils.isBlank(host)) {
            throw new JarbootException("Host is empty!");
        }
        host = host.trim();
        if (trustedHosts.contains(host)) {
            return;
        }
        File file = FileUtils.getFile(TRUSTED_HOSTS_FILE);
        HashSet<String> lines = new HashSet<>(trustedHosts);
        lines.add(host);
        FileUtils.writeLines(file, StandardCharsets.UTF_8.name(), lines, false);
        trustedHosts = lines;
    }

    public static Collection<String> getTrustedHosts() {
        return trustedHosts;
    }

    public static void removeTrustedHost(String host) throws IOException {
        host = host.trim();
        if (StringUtils.isEmpty(host)) {
            throw new JarbootException("Host is empty!");
        }
        if (!trustedHosts.contains(host)) {
            return;
        }
        File file = FileUtils.getFile(TRUSTED_HOSTS_FILE);
        HashSet<String> lines = new HashSet<>(trustedHosts);
        lines.remove(host);
        FileUtils.writeLines(file, StandardCharsets.UTF_8.name(), lines, false);
        trustedHosts = lines;
    }

    private SettingUtils() {

    }
}
