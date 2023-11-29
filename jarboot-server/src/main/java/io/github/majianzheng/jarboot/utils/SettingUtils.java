package io.github.majianzheng.jarboot.utils;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.constant.SettingPropConst;
import io.github.majianzheng.jarboot.api.pojo.SystemSetting;
import io.github.majianzheng.jarboot.common.CacheDirHelper;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.pojo.ResultCodeConst;
import io.github.majianzheng.jarboot.common.utils.OSUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.entity.User;
import io.github.majianzheng.jarboot.security.JwtTokenManager;
import io.github.majianzheng.jarboot.service.UserService;
import io.jsonwebtoken.io.Encoders;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.CollectionUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author majianzheng
 */
public class SettingUtils {
    private static final Logger logger = LoggerFactory.getLogger(SettingUtils.class);

    /** 系统配置缓存 */
    private static final SystemSetting GLOBAL_SETTING = new SystemSetting();
    /** Jarboot配置文件名字 */
    private static final String BOOT_PROPERTIES = "boot.json";
    /** 工作空间属性key */
    private static final String ROOT_DIR_KEY = "jarboot.services.workspace";
    private static final String DEFAULT_JDK_PATH = "jarboot.jdk.path";
    /** 默认VM参数属性key */
    private static final String DEFAULT_VM_OPTS_KEY = "jarboot.services.default-vm-options";
    private static final String MAX_START_TIME = "jarboot.services.max-start-time";
    private static final String MAX_EXIT_TIME = "jarboot.services.max-graceful-exit-time";
    private static final String AFTER_OFFLINE_EXEC = "jarboot.after-server-error-offline";
    private static final String FILE_SHAKE_TIME = "jarboot.file-shake-time";
    private static final String SERVICES_AUTO_START = "jarboot.services.enable-auto-start-after-start";
    /** 默认的工作空间路径 */
    private static String defaultWorkspace;
    /** Jarboot配置文件路径 */
    private static String jarbootConf;
    /** Jarboot的bin文件夹路径 */
    private static String componentsDir;
    /** Jarboot的日志路径 */
    private static String logDir;
    /** jarboot-agent.jar文件的路径 */
    private static String agentJar;
    private static String toolsJar;
    /** file encoding选项 */
    private static final String FILE_ENCODING_OPTION = "-Dfile.encoding=";
    /** 本地地址 */
    private static String localHost = "127.0.0.1:9899";
    private static int port = 9899;
    /** 受信任的远程服务器 */
    private static String trustedHostsFile;
    private static HashSet<String> trustedHosts = new HashSet<>(16);

    private static String homePath = System.getProperty(CommonConst.JARBOOT_HOME);

    private static ApplicationContext context;
    private static UserService userService;
    private static String uuid = StringUtils.EMPTY;

    public static void init(ApplicationContext context, String homePath) {
        SettingUtils.context = context;
        SettingUtils.homePath = homePath;
        int port = context.getEnvironment().getProperty(CommonConst.PORT_KEY, int.class, CommonConst.DEFAULT_PORT);
        SettingUtils.localHost = "127.0.0.1:" + port;
        SettingUtils.port = port;

        final String conf = homePath + File.separator + "conf" + File.separator;
        jarbootConf = conf + "jarboot.properties";
        componentsDir = homePath + File.separator + CommonConst.COMPONENTS_NAME;
        logDir = homePath + File.separator + "logs";
        //jarboot-agent.jar的路径获取
        initAgentJarPath();
        //初始化路径配置，先查找
        initGlobalSetting();
        //初始化受信任服务器列表
        trustedHostsFile = conf + "trusted-hosts.conf";
        initTrustedHosts();
        //初始化默认目录及配置路径
        defaultWorkspace = homePath + File.separator + CommonConst.WORKSPACE;
        userService = context.getBean(UserService.class);
        initEnv();
    }

    private static void initSecretKey() {
        String key = Encoders.BASE64.encode(StringUtils.randomString(60).getBytes(StandardCharsets.UTF_8));
        File file = FileUtils.getFile(jarbootConf);
        HashMap<String, String> props = new HashMap<>(4);
        props.put("jarboot.token.secret.key", key);
        PropertyFileUtils.writeProperty(file, props);
        JwtTokenManager jwtTokenManager = context.getBean(JwtTokenManager.class);
        jwtTokenManager.init(key);
    }

    public static boolean isProd() {
        return Arrays.asList(context.getEnvironment().getActiveProfiles()).contains("prod");
    }

    public static String getHomePath() {
        return homePath;
    }

    public static String getUuid() {
        return uuid;
    }

    public static ApplicationContext getContext() {
        return context;
    }
    /**
     * 初始化Agent路径
     */
    private static void initAgentJarPath() {
        File jarFile = new File(componentsDir, CommonConst.AGENT_JAR_NAME);
        //先尝试从当前路径下获取jar的位置
        if (jarFile.exists()) {
            agentJar = jarFile.getAbsolutePath();
        } else {
            logger.error("文件不存在 {}",  agentJar);
            System.exit(-1);
        }
        jarFile = new File(componentsDir, "jarboot-tools.jar");
        //先尝试从当前路径下获取jar的位置
        if (jarFile.exists()) {
            toolsJar = jarFile.getAbsolutePath();
        } else {
            logger.error("文件不存在 {}", toolsJar);
            System.exit(-1);
        }
    }

    /**
     * 初始化系统配置
     */
    private static void initGlobalSetting() {
        File conf = new File(jarbootConf);
        Properties properties = (conf.exists() && conf.isFile() && conf.canRead()) ?
                PropertyFileUtils.getProperties(conf) : new Properties();
        String workspace = properties.getProperty(ROOT_DIR_KEY, StringUtils.EMPTY);
        if (StringUtils.isNotEmpty(workspace) && !FileUtils.getFile(workspace).exists()) {
            workspace = StringUtils.EMPTY;
        }
        String jdkPath = properties.getProperty(DEFAULT_JDK_PATH, StringUtils.EMPTY);
        if (StringUtils.isNotEmpty(jdkPath) && !FileUtils.getFile(jdkPath).exists()) {
            jdkPath = StringUtils.EMPTY;
        }
        GLOBAL_SETTING.setWorkspace(workspace);
        GLOBAL_SETTING.setJdkPath(jdkPath);
        GLOBAL_SETTING.setDefaultVmOptions(properties.getProperty(DEFAULT_VM_OPTS_KEY, StringUtils.EMPTY).trim());
        int maxStartTime = getDefaultTimeValue(120000, properties, MAX_START_TIME);
        GLOBAL_SETTING.setMaxStartTime(maxStartTime);
        int maxExitTime = getDefaultTimeValue(CommonConst.MAX_WAIT_EXIT_TIME, properties, MAX_EXIT_TIME);
        GLOBAL_SETTING.setMaxExitTime(maxExitTime);
        GLOBAL_SETTING.setAfterServerOfflineExec(properties.getProperty(AFTER_OFFLINE_EXEC, StringUtils.EMPTY).trim());
        int shakeTime = getShakeTime(properties);
        GLOBAL_SETTING.setFileChangeShakeTime(shakeTime);
        GLOBAL_SETTING.setServicesAutoStart(false);
        String autoStartValue = properties.getProperty(SERVICES_AUTO_START, StringUtils.EMPTY);
        if (StringUtils.isNotEmpty(autoStartValue)) {
            try {
                boolean autoStart = Boolean.parseBoolean(autoStartValue);
                GLOBAL_SETTING.setServicesAutoStart(autoStart);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private static int getDefaultTimeValue(int defaultValue, Properties properties, String key) {
        try {
            String value = properties.getProperty(key, StringUtils.EMPTY).trim();
            if (StringUtils.isEmpty(value)) {
                return defaultValue;
            }
            int temp = Integer.parseInt(value);
            if (temp > 0) {
                defaultValue = temp;
            }
        } catch (Exception e) {
            // ignore
        }
        return defaultValue;
    }

    private static int getShakeTime(Properties properties) {
        int shakeTime = 5;
        final int minShakeTime = 3;
        final int maxShakeTime = 600;
        try {
            String value = properties.getProperty(FILE_SHAKE_TIME, StringUtils.EMPTY).trim();
            if (StringUtils.isEmpty(value)) {
                return shakeTime;
            }
            int temp = Integer.parseInt(value);
            if (temp >= minShakeTime && temp <= maxShakeTime) {
                shakeTime = temp;
            }
        } catch (Exception e) {
            // ignore
        }
        return shakeTime;
    }

    private static void initTrustedHosts() {
        File file = FileUtils.getFile(trustedHostsFile);
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
    public static SystemSetting getSystemSetting() {
        return GLOBAL_SETTING;
    }

    /**
     * 更新系统配置
     * @param setting 配置
     */
    public static synchronized void updateSystemSetting(SystemSetting setting) {
        String workspace = setting.getWorkspace();
        if (StringUtils.isNotEmpty(workspace)) {
            File dir = new File(workspace);
            if (!dir.isDirectory() || !dir.exists()) {
                throw new JarbootException(ResultCodeConst.NOT_EXIST, String.format("配置的路径%s不存在！", workspace));
            }
        }
        if (StringUtils.isNotEmpty(setting.getJdkPath())) {
            File javaCmd = FileUtils.getFile(setting.getJdkPath(), CommonConst.BIN_NAME, CommonConst.JAVA_CMD);
            if (!javaCmd.exists()) {
                throw new JarbootException(ResultCodeConst.NOT_EXIST, String.format("%s不存在！", javaCmd.getAbsolutePath()));
            }
        }

        File file = FileUtils.getFile(jarbootConf);
        try {
            HashMap<String, String> props = getSettingPropMap(setting, workspace);
            PropertyFileUtils.writeProperty(file, props);
            //再更新到内存
            if (null == setting.getDefaultVmOptions()) {
                GLOBAL_SETTING.setDefaultVmOptions(StringUtils.EMPTY);
            } else {
                GLOBAL_SETTING.setDefaultVmOptions(setting.getDefaultVmOptions().trim());
            }
            GLOBAL_SETTING.setWorkspace(workspace);
            GLOBAL_SETTING.setJdkPath(setting.getJdkPath());
            GLOBAL_SETTING.setServicesAutoStart(setting.getServicesAutoStart());
            GLOBAL_SETTING.setFileChangeShakeTime(setting.getFileChangeShakeTime());
            GLOBAL_SETTING.setAfterServerOfflineExec(setting.getAfterServerOfflineExec());
            GLOBAL_SETTING.setMaxExitTime(setting.getMaxExitTime());
            GLOBAL_SETTING.setMaxStartTime(setting.getMaxStartTime());
        } catch (Exception e) {
            throw new JarbootException(ResultCodeConst.INTERNAL_ERROR, "更新全局配置文件失败！", e);
        }
    }

    private static HashMap<String, String> getSettingPropMap(SystemSetting setting, String workspace) {
        HashMap<String, String> props = new HashMap<>(8);
        if (null == setting.getDefaultVmOptions()) {
            props.put(DEFAULT_VM_OPTS_KEY, StringUtils.EMPTY);
        } else {
            props.put(DEFAULT_VM_OPTS_KEY, setting.getDefaultVmOptions());
        }
        if (OSUtils.isWindows()) {
            props.put(ROOT_DIR_KEY, FilenameUtils.separatorsToUnix(workspace));
            props.put(DEFAULT_JDK_PATH, FilenameUtils.separatorsToUnix(setting.getJdkPath()));
        } else {
            props.put(ROOT_DIR_KEY, workspace);
            props.put(DEFAULT_JDK_PATH, setting.getJdkPath());
        }
        final int minWait = 1000;
        if (null != setting.getMaxStartTime() && setting.getMaxStartTime() > minWait) {
            props.put(MAX_START_TIME, String.valueOf(setting.getMaxStartTime()));
        } else {
            throw new JarbootException(ResultCodeConst.INVALID_PARAM, "服务启动最长等待时间需大于" + minWait);
        }
        if (null != setting.getMaxExitTime() && setting.getMaxExitTime() > minWait) {
            props.put(MAX_EXIT_TIME, String.valueOf(setting.getMaxExitTime()));
        } else {
            throw new JarbootException(ResultCodeConst.INVALID_PARAM, "服务优雅退出最长等待时间需大于" + minWait);
        }
        final int minShakeTime = 3;
        final int maxShakeTime = 600;
        if (null != setting.getFileChangeShakeTime() && setting.getFileChangeShakeTime() >= minShakeTime && setting.getFileChangeShakeTime() <= maxShakeTime) {
            props.put(FILE_SHAKE_TIME, String.valueOf(setting.getFileChangeShakeTime()));
        } else {
            throw new JarbootException(ResultCodeConst.INVALID_PARAM, "服务文件变更监控抖动时间需在3～600之间！");
        }
        props.put(SERVICES_AUTO_START, String.valueOf(Boolean.TRUE.equals(setting.getServicesAutoStart())));
        props.put(AFTER_OFFLINE_EXEC, setting.getAfterServerOfflineExec());
        return props;
    }

    /**
     * 获取工作空间
     * @return 工作空间
     */
    public static String getWorkspace() {
        String path = GLOBAL_SETTING.getWorkspace();
        if (StringUtils.isBlank(path)) {
            path = defaultWorkspace;
        }
        return path;
    }

    /**
     * 获取日志目录
     * @return 日志目录
     */
    public static String getLogDir() {
        return logDir;
    }

    /**
     * 获取agent的Attach参数
     * @param userDir 用户目录
     * @param serviceName 服务名
     * @param sid 服务唯一id
     * @return 参数
     */
    public static String getAgentStartOption(String userDir, String serviceName, String sid) {
        return new StringBuilder("-javaagent:")
                .append(CommonUtils.getHomeEnv())
                .append(File.separator)
                .append(CommonConst.COMPONENTS_NAME)
                .append(File.separator)
                .append(CommonConst.AGENT_JAR_NAME)
                .append('=')
                .append(getAgentArgs(userDir, serviceName, sid))
                .toString();
    }

    public static String getAgentJar() {
        return agentJar;
    }

    private static String getAgentArgs(String userDir, String serviceName, String sid) {
        final String args = new StringBuilder(64)
                .append(port)
                .append(StringUtils.CR)
                .append(serviceName)
                .append(StringUtils.CR)
                .append(sid)
                .append(StringUtils.CR)
                .append(userDir)
                .toString();
        byte[] bytes = Base64
                .getEncoder()
                .encode(args.getBytes(StandardCharsets.UTF_8));
        return new String(bytes);
    }

    public static String getLocalhost() {
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
     * @param servicePath 服务配置
     * @return jar包路径
     */
    public static String getJarPath(String servicePath) {
        File dir = FileUtils.getFile(servicePath);
        if (!dir.isDirectory() || !dir.exists()) {
            throw new JarbootException("未找到服务" + dir.getName() + "的可执行jar包路径");
        }
        Collection<File> jarList = FileUtils.listFiles(dir, new String[]{CommonConst.JAR_FILE_EXT}, false);
        if (CollectionUtils.isEmpty(jarList)) {
            logger.error("在{}未找到{}服务的jar包", servicePath, dir.getPath());
            throw new JarbootException("未找到服务" + dir.getName() + "的可执行jar包");
        }
        if (jarList.size() > 1) {
            String msg = String.format("在服务%s目录找到了多个jar文件，请配置启动命令！", dir.getName());
            throw new JarbootException(msg);
        }
        if (jarList.iterator().hasNext()) {
            File jarFile = jarList.iterator().next();
            return jarFile.getAbsolutePath().replace(SettingUtils.getHomePath(), CommonUtils.getHomeEnv());
        } else {
            throw new JarbootException("未找到服务" + dir.getName() + "的可执行jar包");
        }
    }

    public static String getCurrentLoginUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    /**
     * 获取服务工作路径
     * @param serviceName 服务名
     * @return 路径
     */
    public static String getServicePath(String serviceName) {
        String userDir = getCurrentUserDir();
        return getServicePath(userDir, serviceName);
    }

    public static String getServicePath(String userDir, String serviceName) {
        return FileUtils.getFile(getWorkspace(), userDir, serviceName).getAbsolutePath();
    }

    public static String getCurrentUserDir() {
        User user = getCurrentLoginUser();
        return StringUtils.isEmpty(user.getUserDir()) ? user.getUsername() : user.getUserDir();
    }

    public static User getCurrentLoginUser() {
        return userService.findUserByUsername(getCurrentLoginUsername());
    }

    /**
     * 获取服务启动配置文件
     * @param path 路径
     * @return 配置文件
     */
    public static File getServiceSettingFile(String path) {
        return FileUtils.getFile(path, BOOT_PROPERTIES);
    }

    /**
     * 获取服务VM选项配置
     * @param servicePath 服务路径
     * @param file 配置文件
     * @return VM选项
     */
    public static String getJvm(String servicePath, String file) {
        if (StringUtils.isBlank(file)) {
            file = SettingPropConst.DEFAULT_VM_FILE;
        }
        Path path = getPath(file);
        if (!path.isAbsolute()) {
            path = getPath(servicePath, file);
        }
        File f = path.toFile();
        String vm = StringUtils.EMPTY;
        if (f.exists()) {
            List<String> lines;
            try {
                lines = FileUtils.readLines(f, StandardCharsets.UTF_8);
            } catch (IOException e) {
                MessageUtils.warn(e.getMessage());
                throw new JarbootException("Read file error.", e);
            }
            vm = lines.stream()
                    //去除首尾空格
                    .map(String::trim)
                    //以#开头的视为注释
                    .filter(line -> !line.startsWith(SettingPropConst.COMMENT_PREFIX))
                    .collect(Collectors.joining(StringUtils.SPACE));
        }
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
     * @param servicePath 服务路径
     * @return sid
     */
    public static String createSid(String servicePath) {
        int p = Math.max(servicePath.lastIndexOf('/'), servicePath.lastIndexOf('\\'));
        String name = servicePath.substring(p + 1);
        String userDir = servicePath.substring(0, p);
        return String.format("service-%08x%08x%08x", uuid.hashCode(), userDir.hashCode(), name.hashCode());
    }

    public static boolean isTrustedHost(String host) {
        if (StringUtils.isBlank(host)) {
            return false;
        }
        return trustedHosts.contains(host);
    }

    public static void addTrustedHost(String host) {
        if (StringUtils.isBlank(host)) {
            throw new JarbootException("Host is empty!");
        }
        host = host.trim();
        if (trustedHosts.contains(host)) {
            return;
        }
        File file = FileUtils.getFile(trustedHostsFile);
        HashSet<String> lines = new HashSet<>(trustedHosts);
        lines.add(host);
        try {
            FileUtils.writeLines(file, StandardCharsets.UTF_8.name(), lines, false);
            trustedHosts = lines;
        } catch (Exception e) {
            throw new JarbootException(e);
        }
    }

    public static Set<String> getTrustedHosts() {
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
        File file = FileUtils.getFile(trustedHostsFile);
        HashSet<String> lines = new HashSet<>(trustedHosts);
        lines.remove(host);
        FileUtils.writeLines(file, StandardCharsets.UTF_8.name(), lines, false);
        trustedHosts = lines;
    }

    public static String getJdkPath() {
        String jdkPath = GLOBAL_SETTING.getJdkPath();
        if (StringUtils.isEmpty(jdkPath)) {
            jdkPath = System.getProperty("java.home");
        }
        return FilenameUtils.separatorsToUnix(jdkPath);
    }

    private static void initEnv() {
        File uuidFile = FileUtils.getFile(homePath, "data", ".uuid");
        boolean isFirst = false;
        // 前缀机器码加路径hash
        final String prefix =  String.format("%s-%08x=", CommonUtils.getMachineCode(), homePath.hashCode());
        if (uuidFile.exists()) {
            try {
                String content = FileUtils.readFileToString(uuidFile, StandardCharsets.UTF_8);
                if (content.startsWith(prefix)) {
                    uuid = content.replace(prefix, StringUtils.EMPTY);
                }
            } catch (Exception e) {
                // ignore
            }
        }
        if (StringUtils.isEmpty(uuid)) {
            isFirst = true;
            // 清理旧进程
            cleanOldProcess();
            uuid = UUID.randomUUID().toString();
            logger.info("初次启动，创建uuid: {}", uuid);
            try {
                FileUtils.writeStringToFile(uuidFile, prefix + uuid, StandardCharsets.UTF_8);
            } catch (Exception e) {
                // ignore
            }
        }
        if (isFirst && isProd()) {
            // 生产模式下，随机生成盐：jarboot.token.secret.key
            initSecretKey();
        }
    }

    private static void cleanOldProcess() {
        File pidDir = CacheDirHelper.getPidDir();
        if (!pidDir.exists()) {
            return;
        }
        Collection<File> pidFiles = FileUtils.listFiles(pidDir, new String[]{"pid"}, true);
        if (CollectionUtils.isEmpty(pidFiles)) {
            return;
        }
        logger.info("机器码发生了改变，清理当前运行的服务进程，{}", pidFiles);
        pidFiles.forEach(file -> {
            try {
                String pid = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                TaskUtils.killByPid(pid);
            } catch (Exception exception) {
                //ignore
            }
            FileUtils.deleteQuietly(file);
        });
    }

    private SettingUtils() {

    }
}
