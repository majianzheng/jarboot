package com.mz.jarboot.utils;

import com.mz.jarboot.constant.ResultCodeConst;
import com.mz.jarboot.constant.SettingConst;
import com.mz.jarboot.exception.MzException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class SettingUtils {
    private static final String CACHE_FILE_NAME_KEY = "cache.file";
    private static final int MAX_TIMEOUT = 3000;
    private static final Logger logger = LoggerFactory.getLogger(SettingUtils.class);

    public static boolean isWindows() {
        String os = System.getProperty("os.name");
        return StringUtils.isNotEmpty(os) && os.startsWith("Windows");
    }

    public static String getCacheFilePath() {
        String path = System.getProperty(SettingConst.WORKSPACE_HOME);
        StringBuilder builder = new StringBuilder();
        String cacheFileName = PropertyFileUtils.getCurrentSetting(CACHE_FILE_NAME_KEY);
        builder.append(path).append(File.separator).
                append(File.separator).append(cacheFileName);
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
        Enumeration<NetworkInterface> ifs = null;
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
     * @throws Exception 异常
     */
    public static void browse1(String url) {
        String osName = System.getProperty("os.name", "");// 获取操作系统的名字
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
        } catch (Exception e) {
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
            logger.warn("警告", "检查到未安装Chrome浏览器，界面显示效果可能会受影响！");
        }
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            throw new MzException(ResultCodeConst.INTERNAL_ERROR, e);
        }
    }

    /**
     * @title 使用默认浏览器打开
     */
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
