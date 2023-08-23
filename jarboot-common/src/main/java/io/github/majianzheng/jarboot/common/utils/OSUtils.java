package io.github.majianzheng.jarboot.common.utils;

import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.pojo.ResultCodeConst;
import org.apache.commons.io.FileUtils;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author majianzheng
 */
@SuppressWarnings({"unused", "java:S3776", "PMD.UndefineMagicConstantRule", "PMD.ClassNamingShouldBeCamelRule"})
public class OSUtils {
    private static final String OPERATING_SYSTEM_NAME = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    private static final String OPERATING_SYSTEM_ARCH = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);
    private static final String UNKNOWN = "unknown";
    private static final int LINUX = 0;
    private static final int MAC_OSX = 1;
    private static final int WINDOWS = 2;
    private static final int UNKNOWN_OS = 3;
    static int platform;

    static String arch;

    static {
        if (OPERATING_SYSTEM_NAME.startsWith("linux")) {
            platform = LINUX;
        } else if (OPERATING_SYSTEM_NAME.startsWith("mac") || OPERATING_SYSTEM_NAME.startsWith("darwin")) {
            platform = MAC_OSX;
        } else if (OPERATING_SYSTEM_NAME.startsWith("windows")) {
            platform = WINDOWS;
        } else {
            platform = UNKNOWN_OS;
        }

        arch = normalizeArch(OPERATING_SYSTEM_ARCH);
    }

    private OSUtils() {
    }

    public static boolean isWindows() {
        return platform == WINDOWS;
    }

    public static boolean isLinux() {
        return platform == LINUX;
    }

    public static boolean isMac() {
        return platform == MAC_OSX;
    }

    public static boolean isCygwinOrMinGW() {
        if (isWindows()) {
            return (System.getenv("MSYSTEM") != null && System.getenv("MSYSTEM").startsWith("MINGW"))
                    || "/bin/bash".equals(System.getenv("SHELL"));
        }
        return false;
    }

    public static String arch() {
        return arch;
    }

    public static boolean isArm32() {
        return "arm_32".equals(arch);
    }

    public static boolean isArm64() {
        return "aarch_64".equals(arch);
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

    /**
     * 读取注册表路径
     * @param nodePath 路径
     * @return 路径下的配置
     */
    public static Map<String, String> readRegistryNode(String nodePath) {
        Map<String, String> regMap = new HashMap<>(16);
        try {
            Process process = Runtime.getRuntime().exec(new String[] {"reg", "query", nodePath});
            process.getOutputStream().close();
            InputStreamReader isr = new InputStreamReader(process.getInputStream());
            String line;
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
            //ignore
        }
        return regMap;
    }

    /**
     * 打开浏览器界面
     * @param url 地址
     */
    public static void browse(String url) {
        if (OSUtils.isWindows()) {
            // windows
            browseInWindows(url);
            return;
        }
        if (OSUtils.isMac()) {
            // Mac
            try {
                Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL", String.class);
                openURL.invoke(null, url);
            } catch (Exception e) {
                throw new JarbootException(ResultCodeConst.INTERNAL_ERROR, e);
            }
            return;
        }
        // Unix or Linux
        String[] browsers = {"firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape"};
        String browser = null;
        try {
            for (int count = 0; count < browsers.length && browser == null; count++) {
                // 这里是如果进程创建成功了，==0是表示正常结束。
                if (Runtime.getRuntime().exec(new String[]{"which", browsers[count]}).waitFor() == 0) {
                    browser = browsers[count];
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JarbootException(ResultCodeConst.INTERNAL_ERROR, e);
        } catch (IOException e) {
            throw new JarbootException(ResultCodeConst.INTERNAL_ERROR, e);
        }
        if (browser == null) {
            throw new JarbootException(ResultCodeConst.INTERNAL_ERROR, "未找到任何可用的浏览器");
        } else {// 这个值在上面已经成功的得到了一个进程。
            try {
                Runtime.getRuntime().exec(new String[]{browser, url});
            } catch (IOException e) {
                throw new JarbootException(ResultCodeConst.INTERNAL_ERROR, e);
            }
        }
    }

    private static void browseInWindows(String url) {
        //检查是否安装了Chrome
        String chromePath = FileUtils.getUserDirectoryPath() +
                "\\AppData\\Local\\Google\\Chrome\\Application\\chrome.exe";
        File file = new File(chromePath);
        String[] cmd = (file.exists() && file.isFile()) ?
                new String[] {chromePath, url} :
                new String[] {"rundll32", "url.dll,FileProtocolHandler", url};
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            throw new JarbootException(ResultCodeConst.INTERNAL_ERROR, e);
        }
    }

    public static void browse2(String url) {
        Desktop desktop = Desktop.getDesktop();
        if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
            URI uri;
            try {
                uri = new URI(url);
                desktop.browse(uri);
            } catch (Exception e) {
                throw new JarbootException(ResultCodeConst.INTERNAL_ERROR, e);
            }
        }
    }

    private static String normalizeArch(String value) {
        value = normalize(value);
        if (value.matches("^(x8664|amd64|ia32e|em64t|x64)$")) {
            return "x86_64";
        }
        if (value.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) {
            return "x86_32";
        }
        if (value.matches("^(ia64w?|itanium64)$")) {
            return "itanium_64";
        }
        if ("ia64n".equals(value)) {
            return "itanium_32";
        }
        if (value.matches("^(sparc|sparc32)$")) {
            return "sparc_32";
        }
        if (value.matches("^(sparcv9|sparc64)$")) {
            return "sparc_64";
        }
        if (value.matches("^(arm|arm32)$")) {
            return "arm_32";
        }
        if ("aarch64".equals(value)) {
            return "aarch_64";
        }
        if (value.matches("^(mips|mips32)$")) {
            return "mips_32";
        }
        if (value.matches("^(mipsel|mips32el)$")) {
            return "mipsel_32";
        }
        if ("mips64".equals(value)) {
            return "mips_64";
        }
        if ("mips64el".equals(value)) {
            return "mipsel_64";
        }
        if (value.matches("^(ppc|ppc32)$")) {
            return "ppc_32";
        }
        if (value.matches("^(ppcle|ppc32le)$")) {
            return "ppcle_32";
        }
        if ("ppc64".equals(value)) {
            return "ppc_64";
        }
        if ("ppc64le".equals(value)) {
            return "ppcle_64";
        }
        if ("s390".equals(value)) {
            return "s390_32";
        }
        if ("s390x".equals(value)) {
            return "s390_64";
        }

        return UNKNOWN;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    }
}
