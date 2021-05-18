package com.mz.jarboot.utils;

import com.mz.jarboot.constant.ResultCodeConst;
import com.mz.jarboot.constant.SettingConst;
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
    private static final String SETTING_CONF_FILE = "jar-boot.properties";
    private static final Map<String, Integer> jvmXmsMap = new HashMap<>();
    private static final Map<String, Integer> jvmXmxMap = new HashMap<>();

    public static Properties getProperties(String file) {
        String configFilePath = System.getProperty(SettingConst.WORKSPACE_HOME) + File.separator + file;
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
        String configFilePath = System.getProperty(SettingConst.WORKSPACE_HOME) + File.separator + SETTING_CONF_FILE;
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

    public static void setCurrentSetting(String file, Map<String, String> propMap) {
        String configFilePath = System.getProperty(SettingConst.WORKSPACE_HOME) + File.separator + file;
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
     * 实时获取ebr-setting.properties配置文件的配置信息
     * @param key 属性名
     * @return 属性值
     */
    public static String getCurrentSetting(String key) {
        Properties properties = getCurrentSettings();
        return properties.getProperty(key);
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

    public static String getEbrConfigPath() {
        String userHome = System.getProperty(SettingConst.WORKSPACE_HOME);
        StringBuilder builder = new StringBuilder();
        builder.append(userHome).append(File.separator);
        return builder.toString();
    }

    /**
     * 写入properties或INI文件属性
     * @param file 属性文件
     * @param props 属性
     */
    public static void writeProperty(File file, Map<String, String> props) {
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

    public static Integer getXmsByModule(String name) {
        if (jvmXmsMap.isEmpty()) {
            initJvmMenMap();
        }
        return jvmXmsMap.get(name);
    }
    public static Integer getXmxByModule(String name) {
        if (jvmXmxMap.isEmpty()) {
            initJvmMenMap();
        }
        return jvmXmxMap.get(name);
    }
    //解析启动优先级配置
    public static Deque<List<String>> parseStartPriority(List<String> servers) {
        Deque<List<String>> result = new ArrayDeque<>();
        String conf = getCurrentSetting(SettingConst.START_PRIORITY_KEY);
        if (StringUtils.isEmpty(conf) || CollectionUtils.isEmpty(servers)) {
            result.offer(null == servers ? new ArrayList<>() : servers);
            return result;
        }
        List<String> clone = new ArrayList<>(servers);
        int l = conf.indexOf('[');
        if (-1 == l) {
            result.offer(clone);
            return result;
        }
        //开始解析
        do {
            int r = conf.indexOf(']', l);
            if (-1 == r) {
                //未找到下一个]，解析失败，则将剩下的servers放到最后
                break;
            }
            int b = (l + 1);
            //取出当前优先级的服务配置
            if (r > b) {
                //解析一个[server1,server2]
                List<String> a = getStrings(conf, clone, r, b);
                if (CollectionUtils.isNotEmpty(a)) {
                    result.offer(a);
                }
            }

            //寻找下一个[]
            l = conf.indexOf('[', r);
        } while (-1 != l);
        //则将剩下的servers放到最后
        if (CollectionUtils.isNotEmpty(clone)) {
            result.offer(clone);
        }
        return result;
    }

    private static List<String> getStrings(String conf, List<String> clone, int r, int b) {
        List<String> a = new ArrayList<>();
        String c = conf.substring(b, r);
        String[] splits = c.split(",");
        for (String s : splits) {
            final String s1 = s.trim();
            boolean has = clone.removeIf(item -> StringUtils.equals(item, s1));
            if (has) {
                a.add(s1);
            }
        }
        return a;
    }

    private static void initJvmMenMap() {
        String jvmMem = getCurrentSetting("jvm-mem");
        if (StringUtils.isEmpty(jvmMem)) {
            return;
        }
        String[] a = jvmMem.split(",");
        if (a.length == 0) {
            return;
        }
        for (String b : a) {
            parseJvmMen(b);
        }
    }

    private static void parseJvmMen(String b) {
        b = b.trim();
        String[] t = b.split(":");
        if (t.length == 2) {
            String[] s = t[1].trim().split("-");
            String xms = "";
            String xmx = "";
            if (s.length == 2) {
                xms = s[0];
                xmx = s[1];
            } else {
                xmx = s[0];
            }
            int x1 = -1;
            int x2 = -1;
            if (StringUtils.isNotEmpty(xms)) {
                x1 = NumberUtils.toInt(xms, -1);
            }
            if (StringUtils.isNotEmpty(xmx)) {
                x2 = NumberUtils.toInt(xmx, -1);
            }
            if (x1 > x2 && x2 > 0) {
                x1 = x2;
            }
            if (x1 > 0) {
                jvmXmsMap.put(t[0], x1);
            }
            if (x2 > 0) {
                jvmXmxMap.put(t[0], x2);
            }
        }
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
