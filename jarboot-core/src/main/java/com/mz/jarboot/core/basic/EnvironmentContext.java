package com.mz.jarboot.core.basic;

import com.mz.jarboot.core.advisor.TransformerManager;
import com.mz.jarboot.core.cmd.Command;
import com.mz.jarboot.core.session.CommandSession;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.session.CommandSessionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The jarboot running environment context.
 * @author jianzhengma
 */
public class EnvironmentContext {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private static String server;
    private static String host;
    private static TransformerManager transformerManager;
    private static ConcurrentMap<String, CommandSession> sessionMap = new ConcurrentHashMap<>();
    private static ConcurrentMap<String, Command> runningCommandMap = new ConcurrentHashMap<>();
    private EnvironmentContext() {}

    public static void init(String server, String host, TransformerManager transformerManager) {
        EnvironmentContext.server = server;
        EnvironmentContext.host = host;
        EnvironmentContext.transformerManager = transformerManager;
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

    public static Command getCurrentCommand(String sessionId) {
        return runningCommandMap.getOrDefault(sessionId, null);
    }

    /**
     * Run the command in running environment, one time one command in one session.
     * @param session
     * @param command
     */
    public static void runCommand(CommandSession session, Command command) {
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
        command.run(session);
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
