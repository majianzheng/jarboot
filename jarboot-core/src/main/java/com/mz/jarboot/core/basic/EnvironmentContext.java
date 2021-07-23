package com.mz.jarboot.core.basic;

import com.mz.jarboot.core.advisor.TransformerManager;
import com.mz.jarboot.core.cmd.AbstractCommand;
import com.mz.jarboot.core.cmd.view.ResultViewResolver;
import com.mz.jarboot.core.server.JarbootBootstrap;
import com.mz.jarboot.core.session.CommandSession;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.session.CommandSessionImpl;
import com.mz.jarboot.core.stream.StdOutStreamReactor;
import com.mz.jarboot.common.JarbootThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.concurrent.*;

/**
 * The jarboot running environment context.
 * @author jianzhengma
 */
public class EnvironmentContext {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private static String server;
    private static String host;
    private static TransformerManager transformerManager;
    private static Instrumentation instrumentation;
    private static ConcurrentMap<String, CommandSession> sessionMap = new ConcurrentHashMap<>(16);
    private static ConcurrentMap<String, AbstractCommand> runningCommandMap = new ConcurrentHashMap<>(16);
    private static ScheduledExecutorService scheduledExecutorService;
    private static String jarbootHome = "./";
    private static ResultViewResolver resultViewResolver;
    private EnvironmentContext() {}

    public static void init(String server, String host, Instrumentation inst) {
        //此时日志还未初始化，在此方法内禁止打印日志信息
        EnvironmentContext.server = server;
        EnvironmentContext.host = host;
        EnvironmentContext.instrumentation = inst;
        EnvironmentContext.transformerManager =  new TransformerManager(inst);
        EnvironmentContext.resultViewResolver = new ResultViewResolver();
        scheduledExecutorService = Executors.newScheduledThreadPool(1,
                JarbootThreadFactory.createThreadFactory("jarboot-sh-cmd"));
        CodeSource codeSource = JarbootBootstrap.class.getProtectionDomain().getCodeSource();
        try {
            File curJar = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
            jarbootHome = curJar.getParent();
        } catch (URISyntaxException e) {
            //ignore
        }
    }

    public static String getJarbootHome() {
        return jarbootHome;
    }

    public static void cleanSession() {
        if (!sessionMap.isEmpty()) {
            sessionMap.forEach((k, v) -> v.cancel());
            sessionMap = new ConcurrentHashMap<>(16);
            StdOutStreamReactor.getInstance().unRegisterAll();
        }
        if (!runningCommandMap.isEmpty()) {
            runningCommandMap.forEach((k, v) -> v.cancel());
            runningCommandMap = new ConcurrentHashMap<>(16);
        }
    }

    public static String getServer() {
        return server;
    }

    public static String getHost() {
        return host;
    }

    public static ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public static TransformerManager getTransformerManager() {
        return transformerManager;
    }

    public static CommandSession registerSession(String sessionId) {
        CommandSession session = sessionMap.getOrDefault(sessionId, null);
        if (null != session) {
            return session;
        }
        synchronized (CommandSession.class) {
            session = new CommandSessionImpl(sessionId);
            sessionMap.put(sessionId, session);
        }
        return session;
    }

    public static ResultViewResolver getResultViewResolver() {
        return resultViewResolver;
    }

    /**
     * 检查job是否已经结束
     * @return 是否结束
     */
    public static boolean checkJobEnd(String sessionId, String jobId) {
        CommandSession session = sessionMap.getOrDefault(sessionId, null);
        if (null == session) {
            return true;
        }
        return !session.getJobId().equals(jobId);
    }

    public static AbstractCommand getCurrentCommand(String sessionId) {
        return runningCommandMap.getOrDefault(sessionId, null);
    }

    /**
     * Run the command in running environment, one time one command in one session.
     * @param command 命令
     */
    public static void runCommand(AbstractCommand command) {
        CommandSession session = command.getSession();
        if (session.isRunning()) {
            AbstractCommand cmd = runningCommandMap.getOrDefault(session.getSessionId(), null);
            if (null == cmd) {
                session.end();
            } else {
                String msg = String.format("当前正在执行%s命令，请等待执行完成，或取消、终止当前命令",
                        command.getName());
                logger.warn(msg);
                return;
            }
        }

        //开始执行命令，更新正在执行的命令
        session.setRunning();
        runningCommandMap.put(session.getSessionId(), command);
        try {
            command.run();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            session.end(false, e.getMessage());
        }
    }

    /**
     * when browser client exit or refreshed it will be reconnect a new session.
     * @param sessionId 会话id
     */
    public static void releaseSession(String sessionId) {
        AbstractCommand command = runningCommandMap.getOrDefault(sessionId, null);
        if (null != command) {
            command.cancel();
            runningCommandMap.remove(sessionId);
        }
        CommandSession session = sessionMap.getOrDefault(sessionId, null);
        if (null != session) {
            session.cancel();
            session.end();
            sessionMap.remove(sessionId);
            StdOutStreamReactor.getInstance().unRegister(sessionId);
        }
    }
}
