package com.mz.jarboot.utils;

import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.constant.SettingPropConst;
import com.mz.jarboot.api.pojo.ServerSetting;
import com.mz.jarboot.common.JarbootException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author majianzheng
 */
public class PropertyFileUtils {
    private static final Logger logger = LoggerFactory.getLogger(PropertyFileUtils.class);
    /** 服务的配置缓存 <sid, 服务配置> */
    private static final Map<String, ServerSetting> SETTING_CACHE = new HashMap<>(16);

    /**
     * 读取properties文件
     * @param file 文件
     * @return {@link Properties}
     */
    public static Properties getProperties(File file) {
        Properties properties = new Properties();
        if (null == file || !file.isFile() || !file.exists()) {
            return properties;
        }
        try (FileInputStream fis = FileUtils.openInputStream(file)) {
            properties.load(fis);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return properties;
    }

    /**
     * 保存properties文件
     * @param file 文件
     * @param properties {@link Properties}
     */
    public static void storeProperties(File file, Properties properties) {
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            properties.store(fos, "Properties file Jarboot created.");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 检查环境变量配置
     * @param env 环境变量
     * @return 是否合法
     */
    public static boolean checkEnvironmentVar(String env) {
        if (StringUtils.isEmpty(env)) {
            return true;
        }
        String[] envs = env.split(CommonConst.COMMA_SPLIT);
        for (String en : envs) {
            //只能包含一个等号，且等号不能在边界
            if (en.length() < 3 && 1 != StringUtils.countMatches(en, CommonConst.EQUAL_CHAR) &&
                    CommonConst.EQUAL_CHAR != en.charAt(0) && CommonConst.EQUAL_CHAR != en.charAt(en.length() - 1)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 根据sid获取服务配置
     * @param sid sid
     * @return 服务配置
     */
    public static ServerSetting getServerSettingBySid(String sid) {
        return SETTING_CACHE.getOrDefault(sid, null);
    }

    /**
     * 根据路径获取服务配置
     * @param serverPath 字符串格式：服务的path
     * @return 服务配置
     */
    public static ServerSetting getServerSetting(String serverPath) {
        int p = serverPath.lastIndexOf(File.separatorChar);
        String name = serverPath.substring(p + 1);
        String sid = SettingUtils.createSid(serverPath);
        File file = SettingUtils.getServerSettingFile(serverPath);
        //判定文件是否更新
        ServerSetting setting = getServerSettingBySid(sid);
        if (null != setting && file.lastModified() == setting.getLastModified()) {
            return setting;
        }

        Properties properties = getProperties(file);
        setting = new ServerSetting(name);
        String group = properties.getProperty(SettingPropConst.GROUP, StringUtils.EMPTY);
        setting.setGroup(group);
        setting.setSid(sid);
        setting.setPath(serverPath);
        String cmd = properties.getProperty(SettingPropConst.COMMAND, StringUtils.EMPTY);
        setting.setCommand(cmd);
        String jvm = properties.getProperty(SettingPropConst.VM, SettingPropConst.DEFAULT_VM_FILE);
        setting.setVm(jvm);
        String args = properties.getProperty(SettingPropConst.ARGS, StringUtils.EMPTY);
        setting.setArgs(args);
        checkAndGetHome(setting, properties);

        //环境变量
        String env = properties.getProperty(SettingPropConst.ENV, StringUtils.EMPTY);
        if (checkEnvironmentVar(env) && StringUtils.isNotEmpty(env)) {
            setting.setEnv(env);
        }

        int priority = NumberUtils.toInt(properties.getProperty(SettingPropConst.PRIORITY,
                SettingPropConst.DEFAULT_PRIORITY));
        setting.setPriority(priority);

        String s = properties.getProperty(SettingPropConst.DAEMON, SettingPropConst.VALUE_TRUE);
        if (StringUtils.equalsIgnoreCase(SettingPropConst.VALUE_FALSE, s)) {
            //初始默认true
            setting.setDaemon(false);
        }

        s = properties.getProperty(SettingPropConst.JAR_UPDATE_WATCH, SettingPropConst.VALUE_TRUE);
        if (StringUtils.equalsIgnoreCase(SettingPropConst.VALUE_FALSE, s)) {
            //初始默认true
            setting.setJarUpdateWatch(false);
        }
        SETTING_CACHE.put(sid, setting);
        return setting;
    }

    private static void checkAndGetHome(ServerSetting setting, Properties properties) {
        //工作目录
        String workHome = properties.getProperty(SettingPropConst.WORK_DIR, StringUtils.EMPTY);
        if (StringUtils.isNotEmpty(workHome)) {
            File dir = new File(workHome);
            if (dir.isDirectory() && dir.exists()) {
                setting.setWorkDirectory(workHome);
            } else {
                //默认启动目录在服务目录，不继承父进程的工作目录
                setting.setWorkDirectory(StringUtils.EMPTY);
            }
        }

        //Jdk路径
        String jdkPath = properties.getProperty(SettingPropConst.JDK_PATH, StringUtils.EMPTY);
        if (StringUtils.isEmpty(jdkPath)) {
            //默认启动目录在服务目录，不继承父进程的工作目录
            setting.setJdkPath(StringUtils.EMPTY);
        } else {
            File dir = new File(jdkPath);
            if (dir.isDirectory() && dir.exists()) {
                setting.setJdkPath(jdkPath);
            } else {
                //默认启动目录在服务目录，不继承父进程的工作目录
                setting.setJdkPath(StringUtils.EMPTY);
            }
        }
    }


    /**
     * 写入properties或INI文件属性（保留注释等内容）
     * @param file 属性文件
     * @param props 属性
     */
    public static void writeProperty(File file, Map<String, String> props) {
        if (null == file || null == props || props.isEmpty()) {
            return;
        }
        if (!file.isFile() || !file.exists()) {
            throw new JarbootException(ResultCodeConst.INTERNAL_ERROR, "配置文件不存在：" + file.getPath());
        }
        List<String> lines = new ArrayList<>();
        try {
            lines = FileUtils.readLines(file, StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            logger.info(e.getMessage(), e);
        }
        HashMap<String, String> copy = new HashMap<>(props);
        if (!CollectionUtils.isEmpty(lines)) {
            for (int i = 0; i < lines.size(); ++i) {
                String line = lines.get(i);
                try {
                    line = parsePropLine(line, copy);
                    lines.set(i, line);
                } catch (JarbootException e) {
                    //do nothing
                }
            }
        }
        if (!copy.isEmpty()) {
            for (Map.Entry<String, String> entry : copy.entrySet()) {
                lines.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
            }
        }
        try {
            FileUtils.writeLines(file, StandardCharsets.UTF_8.name(), lines);
        } catch (IOException e) {
            logger.info(e.getMessage(), e);
        }
    }

    /**
     * 解析启动优先级配置
     * @param servers 服务列表，字符串：服务path
     * @return 优先级排序结果
     */
    public static Queue<ServerSetting> parseStartPriority(List<String> servers) {
        //优先级最大的排在最前面
        PriorityQueue<ServerSetting> queue = new PriorityQueue<>((o1, o2) -> o2.getPriority() - o1.getPriority());
        if (CollectionUtils.isEmpty(servers)) {
            return queue;
        }
        servers.forEach(path -> {
            ServerSetting setting = getServerSetting(path);
            queue.offer(setting);
        });
        return queue;
    }

    /**
     * 解析终止优先级配置，与启动优先级相反
     * @param paths 服务列表，字符串：服务path
     * @return 排序结果
     */
    public static Queue<ServerSetting> parseStopPriority(List<String> paths) {
        //优先级小的排在最前面
        PriorityQueue<ServerSetting> queue = new PriorityQueue<>(Comparator.comparingInt(ServerSetting::getPriority));
        if (CollectionUtils.isEmpty(paths)) {
            return queue;
        }
        paths.forEach(path -> {
            ServerSetting setting = getServerSetting(path);
            queue.offer(setting);
        });
        return queue;
    }

    private static String parsePropLine(String line, Map<String, String> props) {
        line = StringUtils.trim(line);
        if (StringUtils.indexOf(line, CommonConst.EQUAL_CHAR) <= 0 ||
                0 == StringUtils.indexOf(line, SettingPropConst.COMMENT_PREFIX)) {
            throw new JarbootException();
        }
        String[] spliced = StringUtils.split(line, "=", 2);
        if (spliced.length <= 0) {
            throw new JarbootException();
        }
        String key = StringUtils.trim(spliced[0]);
        String value = props.getOrDefault(key, null);
        if (null == value) {
            throw new JarbootException();
        }
        line = key + CommonConst.EQUAL_CHAR + value;
        props.remove(key);
        return line;
    }

    private PropertyFileUtils(){}
}
