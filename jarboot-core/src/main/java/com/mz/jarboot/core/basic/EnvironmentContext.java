package com.mz.jarboot.core.basic;

import com.mz.jarboot.core.advisor.TransformerManager;
import com.mz.jarboot.core.cmd.Command;
import com.mz.jarboot.core.cmd.view.ResultViewResolver;
import com.mz.jarboot.core.server.JarbootBootstrap;
import com.mz.jarboot.core.session.CommandSession;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.session.CommandSessionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

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
    private static ConcurrentMap<String, CommandSession> sessionMap = new ConcurrentHashMap<>();
    private static ConcurrentMap<String, Command> runningCommandMap = new ConcurrentHashMap<>();
    private static ExecutorService executorService;
    private static ScheduledExecutorService scheduledExecutorService;
    private static String jarbootHome = "./";
    private static AtomicLong threadCount = new AtomicLong();
    private static ResultViewResolver resultViewResolver;
    private EnvironmentContext() {}

    public static void init(String server, String host, Instrumentation inst) {
        //此时日志还未初始化，在此方法内禁止打印日志信息
        EnvironmentContext.server = server;
        EnvironmentContext.host = host;
        EnvironmentContext.instrumentation = inst;
        EnvironmentContext.transformerManager =  new TransformerManager(inst);
        EnvironmentContext.resultViewResolver = new ResultViewResolver();
        executorService = new ThreadPoolExecutor(1, 4, 30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(64),
                r -> {
            Thread t = new Thread(r, "jarboot-thread-" + threadCount.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        scheduledExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                final Thread t = new Thread(r, "jarboot-sh-cmd");
                t.setDaemon(true);
                return t;
            }
        });
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
            sessionMap = new ConcurrentHashMap<>();
        }
        if (!runningCommandMap.isEmpty()) {
            runningCommandMap.forEach((k, v) -> v.cancel());
            runningCommandMap = new ConcurrentHashMap<>();
        }
    }

    public static String getServer() {
        return server;
    }

    public static String getHost() {
        return host;
    }

    public static ExecutorService getExecutorService() {
        return executorService;
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
     * @return
     */
    public static boolean checkJobEnd(String sessionId, String jobId) {
        CommandSession session = sessionMap.getOrDefault(sessionId, null);
        if (null == session) {
            return true;
        }
        return !session.getJobId().equals(jobId);
    }

    public static Command getCurrentCommand(String sessionId) {
        return runningCommandMap.getOrDefault(sessionId, null);
    }

    /**
     * Run the command in running environment, one time one command in one session.
     * @param command
     */
    public static void runCommand(Command command) {
        CommandSession session = command.getSession();
        if (session.isRunning()) {
            Command cmd = runningCommandMap.getOrDefault(session.getSessionId(), null);
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
        command.run();
    }

    /**
     * when browser client exit or refreshed it will be reconnect a new session.
     * @param sessionId
     */
    public static void releaseSession(String sessionId) {
        Command command = runningCommandMap.getOrDefault(sessionId, null);
        if (null != command) {
            command.cancel();
            command.complete();
            runningCommandMap.remove(sessionId);
        }
        CommandSession session = sessionMap.getOrDefault(sessionId, null);
        if (null != session) {
            session.cancel();
            session.end();
            sessionMap.remove(sessionId);
        }
    }
}
