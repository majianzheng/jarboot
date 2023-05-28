package com.mz.jarboot.utils;

import com.mz.jarboot.common.pojo.ResultCodeConst;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.constant.SettingPropConst;
import com.mz.jarboot.api.pojo.ServiceSetting;
import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.common.utils.StringUtils;
import org.apache.commons.io.FileUtils;
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
    private static final Map<String, ServiceSetting> SETTING_CACHE = new HashMap<>(16);

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
            int first = en.indexOf(CommonConst.EQUAL_CHAR);
            int last = en.lastIndexOf(CommonConst.EQUAL_CHAR);
            if (en.length() < 3 || first <= 0 || last == en.length() - 1 || first != last) {
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
    public static ServiceSetting getServiceSettingBySid(String sid) {
        return SETTING_CACHE.getOrDefault(sid, null);
    }

    /**
     * 根据服务名获取服务配置
     * @param serviceName 服务名
     * @return 服务配置
     */
    public static ServiceSetting getServiceSetting(String serviceName) {
        return getServiceSettingByPath(SettingUtils.getWorkspace() + File.separator + serviceName);
    }

    /**
     * 根据路径获取服务配置
     * @param serverPath 字符串格式：服务的path
     * @return 服务配置
     */
    public static ServiceSetting getServiceSettingByPath(String serverPath) {
        int p = Math.max(serverPath.lastIndexOf('/'), serverPath.lastIndexOf('\\'));
        String workspace = serverPath.substring(0, p);
        String name = serverPath.substring(p + 1);
        String sid = SettingUtils.createSid(serverPath);
        File file = SettingUtils.getServiceSettingFile(serverPath);
        //判定文件是否更新
        ServiceSetting setting = getServiceSettingBySid(sid);
        if (null != setting && file.lastModified() == setting.getLastModified()) {
            return setting;
        }

        Properties properties = getProperties(file);
        setting = new ServiceSetting(name);
        String group = properties.getProperty(SettingPropConst.GROUP, StringUtils.EMPTY);
        setting.setGroup(group);
        setting.setSid(sid);
        setting.setWorkspace(workspace);
        String cmd = properties.getProperty(SettingPropConst.COMMAND, StringUtils.EMPTY);
        setting.setCommand(cmd);
        String jvm = properties.getProperty(SettingPropConst.VM, SettingPropConst.DEFAULT_VM_FILE);
        setting.setVm(jvm);
        String args = properties.getProperty(SettingPropConst.ARGS, StringUtils.EMPTY);
        setting.setArgs(args);
        String appType  = properties.getProperty(SettingPropConst.APP_TYPE, "java");
        setting.setApplicationType(appType);
        checkAndGetHome(setting, properties);

        setting.setScheduleType(properties.getProperty(SettingPropConst.SCHEDULE_TYPE, SettingPropConst.SCHEDULE_ONCE));

        //环境变量
        String env = properties.getProperty(SettingPropConst.ENV, StringUtils.EMPTY);
        if (checkEnvironmentVar(env) && StringUtils.isNotEmpty(env)) {
            setting.setEnv(env);
        }

        int priority = Integer.parseInt(properties.getProperty(SettingPropConst.PRIORITY,
                SettingPropConst.DEFAULT_PRIORITY));
        setting.setPriority(priority);

        String s = properties.getProperty(SettingPropConst.DAEMON, SettingPropConst.VALUE_TRUE);
        if (SettingPropConst.VALUE_FALSE.equalsIgnoreCase(s)) {
            //初始默认true
            setting.setDaemon(false);
        }

        s = properties.getProperty(SettingPropConst.JAR_UPDATE_WATCH, SettingPropConst.VALUE_TRUE);
        if (SettingPropConst.VALUE_FALSE.equalsIgnoreCase(s)) {
            //初始默认true
            setting.setJarUpdateWatch(false);
        }
        SETTING_CACHE.put(sid, setting);
        return setting;
    }

    private static void checkAndGetHome(ServiceSetting setting, Properties properties) {
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
     * @param serviceNames 服务列表
     * @return 优先级排序结果
     */
    public static Queue<ServiceSetting> parseStartPriority(List<String> serviceNames) {
        //优先级最大的排在最前面
        PriorityQueue<ServiceSetting> queue = new PriorityQueue<>((o1, o2) -> o2.getPriority() - o1.getPriority());
        if (CollectionUtils.isEmpty(serviceNames)) {
            return queue;
        }
        serviceNames.forEach(serviceName -> {
            ServiceSetting setting = getServiceSetting(serviceName);
            queue.offer(setting);
        });
        return queue;
    }

    /**
     * 解析终止优先级配置，与启动优先级相反
     * @param serviceNames 服务列表，字符串：服务path
     * @return 排序结果
     */
    public static Queue<ServiceSetting> parseStopPriority(List<String> serviceNames) {
        //优先级小的排在最前面
        PriorityQueue<ServiceSetting> queue = new PriorityQueue<>(Comparator.comparingInt(ServiceSetting::getPriority));
        if (CollectionUtils.isEmpty(serviceNames)) {
            return queue;
        }
        serviceNames.forEach(serviceName -> {
            ServiceSetting setting = getServiceSetting(serviceName);
            queue.offer(setting);
        });
        return queue;
    }

    private static String parsePropLine(String line, Map<String, String> props) {
        line = line.trim();
        if (line.indexOf(CommonConst.EQUAL_CHAR) <= 0 || line.startsWith(SettingPropConst.COMMENT_PREFIX)) {
            throw new JarbootException();
        }
        String[] spliced = line.split("=", 2);
        if (spliced.length <= 0) {
            throw new JarbootException();
        }
        String key = spliced[0].trim();
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
