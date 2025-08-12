package io.github.majianzheng.jarboot.tools.common;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.ServerRuntimeInfo;
import io.github.majianzheng.jarboot.common.AnsiLog;
import io.github.majianzheng.jarboot.common.CacheDirHelper;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.PidFileHelper;
import io.github.majianzheng.jarboot.common.utils.HttpUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.security.CodeSource;
import java.util.Objects;
import java.util.Properties;

/**
 * @author majianzheng
 */
@Slf4j
public class Utils {
    public static String getJarbootHome() {
        String jarbootHome = System.getenv(CommonConst.JARBOOT_HOME);
        if (StringUtils.isEmpty(jarbootHome)) {
            CodeSource codeSource = Utils.class.getProtectionDomain().getCodeSource();
            try {
                File agentJarFile = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
                jarbootHome = agentJarFile.getParentFile().getParentFile().getPath();
            } catch (Exception e) {
                throw new JarbootException("Get current path failed!" + e.getMessage(), e);
            }
        }
        if (StringUtils.isEmpty(jarbootHome)) {
            throw new JarbootException(CommonConst.JARBOOT_HOME + " env is not set!");
        }
        File home = FileUtils.getFile(jarbootHome);
        if (home.isDirectory()) {
            String homePath = home.getAbsolutePath();
            String cache = homePath + File.separator + ".cache";
            System.setProperty("pty4j.tmpdir", cache);
            System.setProperty("java.io.tmpdir", cache);
            System.setProperty("java.tmp.dir", cache);
            jarbootHome = homePath;
            System.setProperty(CommonConst.JARBOOT_HOME, jarbootHome);
        }
        return jarbootHome;
    }

    public static boolean isStarted() {
        File info = CacheDirHelper.getServerInfoFile();
        if (!info.exists()) {
            return false;
        }
        Properties properties = new Properties();
        try (InputStream is = FileUtils.openInputStream(info)) {
            properties.load(is);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        String infoPid = properties.getProperty("pid");
        String host = properties.getProperty("host");
        if (StringUtils.isEmpty(infoPid) || StringUtils.isEmpty(host)) {
            return false;
        }
        String pid = PidFileHelper.getServerPid();
        if (!Objects.equals(pid, infoPid)) {
            return false;
        }
        if (!host.startsWith(CommonConst.HTTP) && !host.startsWith(CommonConst.HTTPS)) {
            host = CommonConst.HTTP + host;
        }
        String url = host + CommonConst.SERVER_RUNTIME_CONTEXT;
        ServerRuntimeInfo runtimeInfo = HttpUtils.getObj(url, ServerRuntimeInfo.class, null);
        if (null == runtimeInfo) {
            return false;
        }
        AnsiLog.println("Jarboot version：{}", runtimeInfo.getVersion());
        AnsiLog.println("OS: {}", runtimeInfo.getOs() + (Boolean.TRUE.equals(runtimeInfo.getInDocker()) ? " Docker" : ""));
        return true;
    }

    private Utils() {}
}
