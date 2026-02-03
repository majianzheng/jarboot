package io.github.majianzheng.jarboot;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.exception.JarbootRunException;
import io.github.majianzheng.jarboot.api.pojo.ServerRuntimeInfo;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.common.AnsiLog;
import io.github.majianzheng.jarboot.common.CacheDirHelper;
import io.github.majianzheng.jarboot.common.PidFileHelper;
import io.github.majianzheng.jarboot.common.utils.HttpUtils;
import io.github.majianzheng.jarboot.common.utils.VMUtils;
import io.github.majianzheng.jarboot.common.utils.VersionUtils;
import io.github.majianzheng.jarboot.service.TaskWatchService;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.*;
import java.nio.channels.FileLock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * check file is all exist and environment is jdk.
 * @author majianzheng
 */
public class AppEnvironment implements SpringApplicationRunListener {
    private static final Logger logger = LoggerFactory.getLogger(AppEnvironment.class);
    private String homePath;
    private FileLock lock;

    public AppEnvironment(SpringApplication app, String[] args) {}
    @Override
    public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
        //初始化工作目录
        homePath = environment.getProperty(CommonConst.JARBOOT_HOME);
        if (null == homePath || homePath.isEmpty()) {
            homePath = System.getenv(CommonConst.JARBOOT_HOME);
            if (null == homePath) {
                AnsiLog.error("获取JARBOOT_HOME失败！");
                System.exit(-1);
            }
        }

        homePath = FileUtils.getFile(homePath).getAbsolutePath();
        String cache = homePath + File.separator + ".cache";
        System.setProperty("pty4j.tmpdir", cache);
        System.setProperty("java.io.tmpdir", cache);
        System.setProperty("java.tmp.dir", cache);
        System.setProperty(CommonConst.JARBOOT_HOME, homePath);
        final String ver = "v" + VersionUtils.version;
        System.setProperty("application.version", ver);
        //derby数据库驱动的日志文件位置
        final String derbyLog = homePath + File.separator + "logs" + File.separator + "derby.log";
        System.setProperty("derby.stream.error.file", derbyLog);
        System.setProperty("user.dir", homePath);
        // 进程单实例加锁
        this.lock = CacheDirHelper.singleInstanceTryLock();
        if (null == this.lock) {
            AnsiLog.error("Jarboot server is already started!");
            System.exit(-1);
        }
        if (checkPid(environment)) {
            AnsiLog.error("Jarboot server{} is already started!", PidFileHelper.getServerPid());
            System.exit(-1);
        }
        PidFileHelper.writeServerPid();
        //环境初始化
        try {
            // 环境检查
            checkEnvironment();
            //初始化cache目录
            CacheDirHelper.init();
        } catch (Exception e) {
            AnsiLog.error(e);
            System.exit(-1);
        }
    }

    @Override
    public void started(ConfigurableApplicationContext context, Duration timeTaken) {
        CacheDirHelper.getServerPidFile().deleteOnExit();
        // 初始化配置
        SettingUtils.init(context, homePath);
        TaskWatchService taskWatchService = context.getBean(TaskWatchService.class);
        taskWatchService.init();
        // 集群配置初始化
        ClusterClientManager.getInstance().init();
        saveInfo();
    }

    private static void saveInfo() {
        String host = SettingUtils.getLocalhost();
        String serverPid = PidFileHelper.getServerPid();
        Properties properties = new Properties();
        properties.setProperty(CommonConst.HOST_KEY, host);
        properties.setProperty("pid", serverPid);
        try (OutputStream os = FileUtils.newOutputStream(CacheDirHelper.getServerInfoFile(), false)) {
            properties.store(os, "Jarboot server info file created by auto.");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {
        if (null != this.lock) {
            try {
                this.lock.release();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void checkEnvironment() {
        String binDir = homePath + File.separator + CommonConst.COMPONENTS_NAME;
        //先检查jarboot-agent.jar文件
        checkFile(binDir, CommonConst.AGENT_JAR_NAME);
        //检查jarboot-core.jar文件，该文件必须和jarboot-agent.jar处于同一目录下
        checkFile(binDir, "jarboot-core.jar");
        checkFile(binDir, "jarboot-spy.jar");

        //检查是否是jdk环境，是否存在tools.jar
        if (VMUtils.getInstance().check()) {
            return;
        }
        throw new JarbootRunException("检查环境错误，当前运行环境未安装jdk，请检查环境变量是否配置，可能是使用了jre环境。");
    }

    private static void checkFile(String dir, String fileName) {
        File file = new File(dir, fileName);
        if (!file.exists()) {
            throw new JarbootRunException("检查环境错误，未发现" + fileName);
        }
        if (!file.isFile()) {
            throw new JarbootRunException(String.format("检查环境错误，%s不是文件类型。", fileName));
        }
    }

    private boolean checkPid(ConfigurableEnvironment environment) {
        String serverPid = PidFileHelper.getServerPid();
        Map<String, String> vms = VMUtils.getInstance().listVM();
        if (vms.containsKey(serverPid)) {
            int port = environment.getProperty(CommonConst.PORT_KEY, int.class, CommonConst.DEFAULT_PORT);
            String localHost = "http://127.0.0.1:" + port;
            String url = localHost + CommonConst.SERVER_RUNTIME_CONTEXT;
            try {
                ServerRuntimeInfo runtimeInfo = HttpUtils.getObj(url, ServerRuntimeInfo.class, null);
                if (null == runtimeInfo) {
                    return false;
                }
                return Objects.equals(runtimeInfo.getPid(), serverPid);
            } catch (Exception e) {
                AnsiLog.error(e);
                return false;
            }
        } else {
            return false;
        }
    }
}
