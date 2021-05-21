package com.mz.jarboot.utils;

import com.mz.jarboot.constant.ResultCodeConst;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.dto.ServerSettingDTO;
import com.mz.jarboot.exception.MzException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PropertyFileUtils {
    private static final Logger logger = LoggerFactory.getLogger(PropertyFileUtils.class);
    private static final String SETTING_CONF_FILE = "jarboot.properties";

    public static Properties getProperties(String file) {
        String configFilePath = System.getProperty(CommonConst.WORKSPACE_HOME) + File.separator + file;
        File configFile = FileUtils.getFile(configFilePath);
        return getProperties(configFile);
    }
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

    public static void setCurrentSetting(String key, String value) {
        String configFilePath = System.getProperty(CommonConst.WORKSPACE_HOME) + File.separator + SETTING_CONF_FILE;
        File configFile = FileUtils.getFile(configFilePath);
        if (configFile.exists() && configFile.isFile()) {
            Map<String, String> propMap = new HashMap<>();
            propMap.put(key, value);
            writeProperty(configFile, propMap);
        }
    }

    public static void setCurrentSetting(String file, String key, String value) {
        Map<String, String> propMap = new HashMap<>();
        propMap.put(key, value);
        setCurrentSetting(file, propMap);
    }

    private static void setCurrentSetting(String file, Map<String, String> propMap) {
        String configFilePath = System.getProperty(CommonConst.WORKSPACE_HOME) + File.separator + file;
        File configFile = FileUtils.getFile(configFilePath);
        if (configFile.exists() && configFile.isFile()) {
            writeProperty(configFile, propMap);
        }
    }

    /**
     * 实时获取ebr-setting.properties配置文件的配置信息
     * @return 属性值
     */
    public static Properties getCurrentSettings() {
        return getProperties(SETTING_CONF_FILE);
    }

    /**
     * 实时获取jarboot.properties配置文件的配置信息
     * @param key 属性名
     * @return 属性值
     */
    public static String getCurrentSetting(String key) {
        Properties properties = getCurrentSettings();
        return properties.getProperty(key);
    }

    public static ServerSettingDTO getServerSetting(String server) {
        ServerSettingDTO setting = new ServerSettingDTO(server);
        String path = SettingUtils.getServerSettingFilePath(server);
        Properties properties = getProperties(path);
        if (properties.isEmpty()) {
            return setting;
        }
        String jvm = properties.getProperty("jvm", "");
        setting.setJvm(jvm);
        String args = properties.getProperty("args", "");
        setting.setArgs(args);
        int priority = NumberUtils.toInt(properties.getProperty("priority", "1"), 1);
        setting.setPriority(priority);
        String s = properties.getProperty("daemon", "true");
        if (StringUtils.equalsIgnoreCase("false", s)) {
            //初始默认true
            setting.setDaemon(false);
        }
        s = properties.getProperty("jarUpdateWatch", "true");
        if (StringUtils.equalsIgnoreCase("false", s)) {
            //初始默认true
            setting.setJarUpdateWatch(false);
        }
        return setting;
    }

    /**
     * 实时获取.properties配置文件的配置信息
     * @param key 属性名
     * @return 属性值
     */
    public static String getCurrentSetting(String file, String key) {
        Properties properties = getProperties(file);
        return properties.getProperty(key);
    }

    public static String getCurrentSetting(File file, String key) {
        if (null == file || !file.exists() || !file.isFile()) {
            return "";
        }
        Properties properties = new Properties();
        try (FileInputStream fis = FileUtils.openInputStream(file)) {
            properties.load(fis);
        } catch (Exception e) {
            //ignore
        }
        return properties.getProperty(key);
    }
    

    /**
     * 写入properties或INI文件属性
     * @param file 属性文件
     * @param props 属性
     */
    private static void writeProperty(File file, Map<String, String> props) {
        if (null == file || MapUtils.isEmpty(props)) {
            return;
        }
        if (!file.isFile() || !file.exists()) {
            throw new MzException(ResultCodeConst.INTERNAL_ERROR, "配置文件不存在：" + file.getPath());
        }
        List<String> lines = null;
        try {
            lines = FileUtils.readLines(file, StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            logger.info(e.getMessage(), e);
        }
        if (CollectionUtils.isEmpty(lines)) {
            return;
        }
        for (int i = 0; i < lines.size(); ++i) {
            String line = lines.get(i);
            try {
                line = parsePropLine(line, props);
                lines.set(i, line);
            } catch (MzException e) {
                //do nothing
            }
        }
        try {
            FileUtils.writeLines(file, StandardCharsets.UTF_8.name(), lines);
        } catch (IOException e) {
            logger.info(e.getMessage(), e);
        }
    }

    //解析启动优先级配置
    public static Queue<ServerSettingDTO> parseStartPriority(List<String> servers) {
        //优先级最大的排在最前面
        Queue<ServerSettingDTO> queue = new PriorityQueue<>(Comparator.comparingInt(ServerSettingDTO::getPriority));
        if (CollectionUtils.isEmpty(servers)) {
            return queue;
        }
        servers.forEach(server -> {
            ServerSettingDTO setting = getServerSetting(server);
            queue.offer(setting);
        });
        return queue;
    }

    //解析终止优先级配置，与启动优先级相反
    public static Queue<ServerSettingDTO> parseStopPriority(List<String> servers) {
        //优先级小的排在最前面
        Queue<ServerSettingDTO> queue = new PriorityQueue<>((o1, o2) -> o2.getPriority() - o1.getPriority());
        if (CollectionUtils.isEmpty(servers)) {
            return queue;
        }
        servers.forEach(server -> {
            ServerSettingDTO setting = getServerSetting(server);
            queue.offer(setting);
        });
        return queue;
    }

    private static String parsePropLine(String line, Map<String, String> props) {
        line = StringUtils.trim(line);
        if (StringUtils.indexOf(line, '=') <= 0 || 0 == StringUtils.indexOf(line, '#')) {
            throw new MzException();
        }
        String[] spliced = StringUtils.split(line, "=", 2);
        if (spliced.length <= 0) {
            throw new MzException();
        }
        String key = StringUtils.trim(spliced[0]);
        String value = props.get(key);
        if (null != value) {
            line = key + '=' + value;
            return line;
        }
        if (spliced.length < 2) {
            throw new MzException();
        }
        value = StringUtils.trim(spliced[1]);
        if (StringUtils.isEmpty(value)) {
            throw new MzException();
        }
        line = key + '=' + value;
        return line;
    }

    private PropertyFileUtils(){}
}
