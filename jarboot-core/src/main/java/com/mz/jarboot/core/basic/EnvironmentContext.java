package com.mz.jarboot.core.basic;

import com.mz.jarboot.common.*;
import com.mz.jarboot.core.advisor.TransformerManager;
import com.mz.jarboot.core.cmd.AbstractCommand;
import com.mz.jarboot.core.session.CommandCoreSession;
import com.mz.jarboot.core.session.CommandSessionImpl;
import com.mz.jarboot.core.utils.LogUtils;
import org.slf4j.Logger;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.*;

/**
 * The jarboot running environment context.
 * @author majianzheng
 */
public class EnvironmentContext {
    private static Logger logger = LogUtils.getLogger();

    /** 客户端信息 */
    private static ClientData clientData;
    /** 是否初始化 */
    private static boolean initialized = false;
    /** transformerManager */
    private static TransformerManager transformerManager;
    /** instrumentation用于类查找、增强 */
    private static Instrumentation instrumentation;
    /** 连接会话 */
    private static ConcurrentMap<String, CommandCoreSession> sessionMap = new ConcurrentHashMap<>(16);
    /** 正在执行的命令 */
    private static ConcurrentMap<String, AbstractCommand> runningCommandMap = new ConcurrentHashMap<>(16);
    /** Schedule线程调度 */
    private static ScheduledExecutorService scheduledExecutorService;
    /** Jarboot工作目录 */
    private static String jarbootHome = ".";

    /**
     * 环境初始化
     * @param home 工作目录
     * @param clientData 客户端数据
     * @param inst {@link Instrumentation}
     */
    public static synchronized void init(String home, ClientData clientData, Instrumentation inst) {
        logger = LogUtils.getLogger();
        //此时日志还未初始化，在此方法内禁止打印日志信息
        if (null != home) {
            EnvironmentContext.jarbootHome = home;
        }
        if (null != clientData) {
            EnvironmentContext.clientData = clientData;
        }
        if (null != inst) {
            EnvironmentContext.instrumentation = inst;
        }
        EnvironmentContext.transformerManager =  new TransformerManager(EnvironmentContext.instrumentation);

        int coreSize = Math.max(Runtime.getRuntime().availableProcessors() / 2, 4);
        scheduledExecutorService = Executors.newScheduledThreadPool(coreSize,
                JarbootThreadFactory.createThreadFactory("jarboot-sh-cmd", true));
        initialized = true;
    }

    public static synchronized void destroy() {
        cleanSession();
        scheduledExecutorService.shutdown();
        EnvironmentContext.clientData = null;
        EnvironmentContext.transformerManager.destroy();
        EnvironmentContext.transformerManager = null;
        scheduledExecutorService = null;
        initialized = false;
    }

    /**
     * 获取工作目录
     * @return 工作目录
     */
    public static String getJarbootHome() {
        return jarbootHome;
    }

    /**
     * 清理会话，用于连接重置时
     */
    public static void cleanSession() {
        if (!sessionMap.isEmpty()) {
            sessionMap.forEach((k, v) -> v.cancel());
            sessionMap = new ConcurrentHashMap<>(16);
        }
        if (!runningCommandMap.isEmpty()) {
            runningCommandMap.forEach((k, v) -> v.cancel());
            runningCommandMap = new ConcurrentHashMap<>(16);
        }
    }

    /**
     * 是否初始化
     * @return 是否初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }

    public static ClientData getClientData() {
        return clientData;
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

    public static CommandCoreSession registerSession(String sessionId) {
        return sessionMap.computeIfAbsent(sessionId, key -> new CommandSessionImpl(sessionId));
    }

    /**
     * 检查job是否已经结束
     * @return 是否结束
     */
    public static boolean checkJobEnd(String sessionId, String jobId) {
        CommandCoreSession session = sessionMap.getOrDefault(sessionId, null);
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
        if (null == command) {
            return;
        }
        final CommandCoreSession session = command.getSession();
        if (checkCommandRunning(session)) {
            return;
        }
        scheduledExecutorService.execute(() -> {
            if (checkCommandRunning(session)) {
                return;
            }
            //开始执行命令，更新正在执行的命令
            session.setRunning();
            runningCommandMap.put(session.getSessionId(), command);
            try {
                command.run();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                session.end(false, e.getMessage());
                String msg = "命令（" + command.getName() + "）执行失败！<br>" + e.getMessage();
                AgentServiceOperator.noticeError(msg, session.getSessionId());
            }
        });
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
        CommandCoreSession session = sessionMap.getOrDefault(sessionId, null);
        if (null != session) {
            session.cancel();
            session.end();
            sessionMap.remove(sessionId);
        }
    }

    private static boolean checkCommandRunning(final CommandCoreSession session) {
        if (session.isRunning()) {
            AbstractCommand cmd = runningCommandMap.getOrDefault(session.getSessionId(), null);
            if (null == cmd) {
                session.end();
                return false;
            } else {
                String msg = String.format("当前正在执行%s命令，请等待执行完成，或取消、终止当前命令",
                        cmd.getName());
                AgentServiceOperator.noticeInfo(msg, session.getSessionId());
                return true;
            }
        } else {
            return false;
        }
    }
    private EnvironmentContext() {}
}
