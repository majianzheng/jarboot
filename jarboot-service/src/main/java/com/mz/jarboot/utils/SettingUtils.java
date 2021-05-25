package com.mz.jarboot.utils;

import com.alibaba.fastjson.JSONObject;
import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.common.MzException;
import com.mz.jarboot.dto.GlobalSettingDTO;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.security.CodeSource;
import java.util.*;

public class SettingUtils {
    private static final int MAX_TIMEOUT = 3000;
    private static final Logger logger = LoggerFactory.getLogger(SettingUtils.class);
    private static final String OS_NAME = "os.name";
    private static final String AGENT_JAR_NAME = "jarboot-agent.jar";
    private static GlobalSettingDTO globalSetting = null;
    private static final String BOOT_INI = "boot.ini";
    private static final String GLOBAL_SETTING_FILE = "global-setting.conf";
    private static String agentJar;
    static {
        //jarboot-agent.jar的路径获取
        initAgentJarPath();
        //初始化路径配置，先查找
        initGlobalSetting();
        //进行默认配置
        initDefaultSetting();
    }
    private static void initAgentJarPath() {
        CodeSource codeSource = SettingUtils.class.getProtectionDomain().getCodeSource();
        try {
            File agentJarFile = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
            File jarFile = new File(agentJarFile.getParentFile(), AGENT_JAR_NAME);
            //先尝试从当前路径下获取jar的位置
            if (jarFile.exists()) {
                agentJar = jarFile.getPath();
            } else {
                agentJar = System.getProperty(CommonConst.WORKSPACE_HOME) + File.separator + AGENT_JAR_NAME;
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
        String conf = System.getProperty(CommonConst.WORKSPACE_HOME) + File.separator + GLOBAL_SETTING_FILE;
        File file = FileUtils.getFile(conf);
        if (file.isDirectory()) {
            logger.error("存在与配置文件相同的文件夹名称！");
            return;
        }
        if (!file.exists()) {
            //不存在配置文件
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            globalSetting = (GlobalSettingDTO) ois.readObject();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    private static void initDefaultSetting() {
        if (null == globalSetting) {
            globalSetting = new GlobalSettingDTO();
        }
        String servicesPath = globalSetting.getServicesPath();
        if (StringUtils.isEmpty(servicesPath)) {
            servicesPath = System.getProperty(CommonConst.WORKSPACE_HOME) + File.separator + "services";
            globalSetting.setServicesPath(servicesPath);
        }
    }

    public static GlobalSettingDTO getGlobalSetting() {
        return globalSetting;
    }

    public static void updateGlobalSetting(GlobalSettingDTO setting) {
        String servicesPath = setting.getServicesPath();
        if (StringUtils.isEmpty(servicesPath)) {
            setting.setServicesPath(globalSetting.getServicesPath());
        } else {
            File dir = new File(servicesPath);
            if (!dir.isDirectory() || !dir.exists()) {
                throw new MzException(ResultCodeConst.NOT_EXIST, String.format("配置的路径%s不存在！", servicesPath));
            }
        }

        String arthasHome = setting.getArthasHome();
        if (StringUtils.isEmpty(arthasHome)) {
            setting.setArthasHome(globalSetting.getArthasHome());
        } else {
            File dir = new File(arthasHome);
            if (!dir.isDirectory() || !dir.exists()) {
                throw new MzException(ResultCodeConst.NOT_EXIST, String.format("配置的路径%s不存在！", servicesPath));
            }
        }

        String conf = System.getProperty(CommonConst.WORKSPACE_HOME) + File.separator + GLOBAL_SETTING_FILE;
        File file = FileUtils.getFile(conf);
        try (ObjectOutputStream oo = new ObjectOutputStream(new FileOutputStream(file))){
            if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
            }
            oo.writeObject(file);
            //再更新到内存
            globalSetting = setting;
        } catch (Exception e) {
            throw new MzException(ResultCodeConst.INTERNAL_ERROR, "更新全局配置文件失败！", e);
        }
    }


    public static String getServicesPath() {
        return globalSetting.getServicesPath();
    }
    /**
     * 判断是否Windows系统
     * @return 是否Windows
     */
    public static boolean isWindows() {
        String os = System.getProperty(OS_NAME);
        return StringUtils.isNotEmpty(os) && os.startsWith("Windows");
    }

    /**
     * 判断是否为MacOS系统
     * @return 是否MacOS系统
     */
    public static boolean isMacOS() {
        String os = System.getProperty(OS_NAME);
        return StringUtils.isNotEmpty(os) && os.startsWith("Mac");
    }

    public static void initTargetVM(String server, int pid) {
        Object vm;
        try {
            vm = VMUtils.getInstance().attachVM(pid);
        } catch (Exception e) {
            //ignore
            return;
        }
        try {
            VMUtils.getInstance().loadAgentToVM(vm, agentJar, getAgentArgs(server));
        } catch (Exception e) {
            //ignore
        } finally {
            if (null != vm) {
                VMUtils.getInstance().detachVM(vm);
            }
        }
    }

    public static String getAgentStartOption(String server) {
        return "-javaagent:" + agentJar + "=" + getAgentArgs(server);
    }

    private static String getAgentArgs(String server) {
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
            WebSocketManager.getInstance().noticeWarn("未找到服务" + server + "的可执行jar包路径");
        }
        String[] extensions = {"jar"};
        Collection<File> jarList = FileUtils.listFiles(dir, extensions, false);
        if (org.apache.commons.collections.CollectionUtils.isEmpty(jarList)) {
            logger.error("在{}未找到{}服务的jar包", server, dir.getPath());
            WebSocketManager.getInstance().noticeWarn("未找到服务" + server + "的可执行jar包");
        }
        if (jarList.size() > 1) {
            WebSocketManager.getInstance().noticeError("在服务目录找到了多个jar包！可能会导致服务不可用，请先清理该目录！留下一个可用的jar包文件！");
        }
        if (jarList.iterator().hasNext()) {
            File jarFile = jarList.iterator().next();
            return jarFile.getPath();
        }
        return "";
    }

    public static String getServerPath(String server) {
        return globalSetting.getServicesPath() + File.separator + server;
    }

    public static String getServerSettingFilePath(String server) {
        StringBuilder builder = new StringBuilder();
        builder.append(globalSetting.getServicesPath()).append(File.separator).append(server).append(File.separator).append(BOOT_INI);
        return builder.toString();
    }

    /**
     * 检查host是否能ping通
     * @param host host地址
     * @return 是否ping通
     */
    public static boolean hostReachable(String host) {
        if (StringUtils.isEmpty(host)) {
            return false;
        }
        try {
            return InetAddress.getByName(host).isReachable(MAX_TIMEOUT);
        } catch (Exception e) {
            //ignore
        }
        return false;
    }

    /**
     * 检查host是否是本地的
     * @param host host地址
     * @return 是否本地
     */
    public static boolean hostLocal(String host) {
        if (StringUtils.isEmpty(host)) {
            return false;
        }
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(host);
        } catch (Exception e) {
            //ignore
        }
        if (null == inetAddress) {
            return false;
        }
        if (inetAddress.isLoopbackAddress()) {
            throw new MzException(ResultCodeConst.INVALID_PARAM, "请填写真实IP地址或域名，而不是环路地址：" + host);
        }
        Enumeration<NetworkInterface> ifs;
        try {
            ifs = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new MzException(ResultCodeConst.INTERNAL_ERROR, e);
        }
        while (ifs.hasMoreElements()) {
            Enumeration<InetAddress> addrs = ifs.nextElement().getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress addr = addrs.nextElement();
                if (inetAddress.equals(addr)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isHostConnectable(String host, String port) {
        int p = NumberUtils.toInt(port, -1);
        if (-1 == p) {
            return false;
        }
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, p));
        } catch (Exception e) {
            return false;
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                //ignore
            }
        }
        return true;
    }
    /**
     * 打开浏览器界面
     * @param url 地址
     */
    public static void browse1(String url) {
        String osName = System.getProperty(OS_NAME, "");// 获取操作系统的名字
        if (osName.startsWith("Windows")) {// windows
            browseInWindows(url);
            return;
        }
        if (osName.startsWith("Mac OS")) {// Mac
            try {
                Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL", String.class);
                openURL.invoke(null, url);
            } catch (Exception e) {
                throw new MzException(ResultCodeConst.INTERNAL_ERROR, e);
            }
            return;
        }
        // Unix or Linux
        String[] browsers = {"firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape"};
        String browser = null;
        try {
            for (int count = 0; count < browsers.length && browser == null; count++) { // 执行代码，在brower有值后跳出，
                // 这里是如果进程创建成功了，==0是表示正常结束。
                if (Runtime.getRuntime().exec(new String[]{"which", browsers[count]}).waitFor() == 0) {
                    browser = browsers[count];
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MzException(ResultCodeConst.INTERNAL_ERROR, e);
        } catch (IOException e) {
            throw new MzException(ResultCodeConst.INTERNAL_ERROR, e);
        }
        if (browser == null) {
            throw new MzException(ResultCodeConst.INTERNAL_ERROR, "未找到任何可用的浏览器");
        } else {// 这个值在上面已经成功的得到了一个进程。
            try {
                Runtime.getRuntime().exec(new String[]{browser, url});
            } catch (IOException e) {
                throw new MzException(ResultCodeConst.INTERNAL_ERROR, e);
            }
        }
    }

    private static void browseInWindows(String url) {
        //检查是否安装了Chrome
        String chromePath = FileUtils.getUserDirectoryPath() +
                "\\AppData\\Local\\Google\\Chrome\\Application\\chrome.exe";
        File file = new File(chromePath);
        String cmd;
        if (file.exists() && file.isFile()) {
            cmd = chromePath + " " + url;
        } else {
            cmd = "rundll32 url.dll,FileProtocolHandler " + url;
            logger.warn("检查到未安装Chrome浏览器，界面显示效果可能会受影响！");
        }
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            throw new MzException(ResultCodeConst.INTERNAL_ERROR, e);
        }
    }

    public static void browse2(String url) {
        Desktop desktop = Desktop.getDesktop();
        if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
            URI uri = null;
            try {
                uri = new URI(url);
                desktop.browse(uri);
            } catch (Exception e) {
                throw new MzException(ResultCodeConst.INTERNAL_ERROR, e);
            }
        }
    }

    public static Map<String, String> readRegistryNode(String nodePath) {
        Map<String, String> regMap = new HashMap<>();
        try {
            Process process = Runtime.getRuntime().exec("reg query " + nodePath);
            process.getOutputStream().close();
            InputStreamReader isr = new InputStreamReader(process.getInputStream());
            String line = null;
            BufferedReader ir = new BufferedReader(isr);
            while ((line = ir.readLine()) != null) {
                //连续空格替换单空格
                line = line.trim();
                line = line.replaceAll("\\s{2,}", " ");
                String[] arr = line.split(" ");
                if(arr.length != 3){
                    continue;
                }
                regMap.put(arr[0], arr[2]);
            }
            process.destroy();
        } catch (IOException e) {
            logger.error("错误, 读取注册表失败！" + nodePath, e);
        }
        return regMap;
    }

    /**
     * 读取注册表
     * @param nodePath 路径
     * @param key 主键
     * @return 值
     */
    public static String readRegistryValue(String nodePath, String key) {
        Map<String, String> regMap = readRegistryNode(nodePath);
        return regMap.get(key);
    }

    private SettingUtils() {

    }
}
