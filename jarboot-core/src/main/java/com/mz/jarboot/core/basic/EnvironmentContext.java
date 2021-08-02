package com.mz.jarboot.core.basic;

import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.spi.CommandProcessor;
import com.mz.jarboot.common.*;
import com.mz.jarboot.core.advisor.TransformerManager;
import com.mz.jarboot.core.cmd.AbstractCommand;
import com.mz.jarboot.core.cmd.CommandBuilder;
import com.mz.jarboot.core.cmd.view.ResultViewResolver;
import com.mz.jarboot.core.server.JarbootBootstrap;
import com.mz.jarboot.core.session.CommandCoreSession;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.session.CommandSessionImpl;
import com.mz.jarboot.core.stream.ResultStreamDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * The jarboot running environment context.
 * @author majianzheng
 */
public class EnvironmentContext {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);

    private static String server;
    private static String host;
    private static TransformerManager transformerManager;
    private static Instrumentation instrumentation;
    private static ConcurrentMap<String, CommandCoreSession> sessionMap = new ConcurrentHashMap<>(16);
    private static ConcurrentMap<String, AbstractCommand> runningCommandMap = new ConcurrentHashMap<>(16);
    private static ScheduledExecutorService scheduledExecutorService;
    private static String jarbootHome = "./";
    private static ResultViewResolver resultViewResolver;
    private static volatile boolean started = false;
    private static final String SET_STARTED_API = "/api/public/agent/setStarted?server=";
    private static String setStartedUrl = null;
    private static ResultStreamDistributor distributor = null;
    private EnvironmentContext() {}

    public static void init(String server, String host, Instrumentation inst) {
        //此时日志还未初始化，在此方法内禁止打印日志信息
        EnvironmentContext.server = server;
        EnvironmentContext.host = host;
        EnvironmentContext.instrumentation = inst;
        EnvironmentContext.transformerManager =  new TransformerManager(inst);
        EnvironmentContext.resultViewResolver = new ResultViewResolver();
        setStartedUrl = SET_STARTED_API + server;

        scheduledExecutorService = Executors.newScheduledThreadPool(5,
                JarbootThreadFactory.createThreadFactory("jarboot-sh-cmd", true));
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

    public static CommandCoreSession registerSession(String sessionId) {
        return sessionMap.computeIfAbsent(sessionId, key -> new CommandSessionImpl(sessionId));
    }

    public static ResultViewResolver getResultViewResolver() {
        return resultViewResolver;
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
        CommandCoreSession session = command.getSession();
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
        CommandCoreSession session = sessionMap.getOrDefault(sessionId, null);
        if (null != session) {
            session.cancel();
            session.end();
            sessionMap.remove(sessionId);
        }
    }

    /**
     * 初始化Spring容器中的{@link CommandProcessor}的bean<br>
     * 前置条件：引入了spring-boot-starter-jarboot的依赖
     * @param context Spring Context
     */
    @SuppressWarnings("all")
    public static void springContextInit(Object context) {
        Map<String, CommandProcessor> beans = null;
        //获取
        try {
            beans = (Map<String, CommandProcessor>)context.getClass()
                    .getMethod("getBeansOfType", java.lang.Class.class)
                    .invoke(context, CommandProcessor.class);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
        if (null == beans || beans.isEmpty()) {
            return;
        }
        beans.forEach((k, v) -> {
            //未使用Name注解定义命令时，以bean的Name作为命令名
            String cmd = k;
            Name name = v.getClass().getAnnotation(Name.class);
            if (!(null == name || null == name.value() || name.value().isEmpty())) {
                cmd = name.value();
            }
            if (CommandBuilder.EXTEND_MAP.containsKey(cmd)) {
                //命令重复
                logger.warn("User-defined command {} is repetitive in spring boot.", k);
                return;
            }
            CommandBuilder.EXTEND_MAP.put(cmd, v);
        });
    }

    /**
     * 设置启动成功
     */
    public static void setStarted(){
        if (started) {
            return;
        }
        com.mz.jarboot.core.utils.HttpUtils.getSimple(setStartedUrl);
        started = true;
    }

    public static void setDistributor(ResultStreamDistributor distributor) {
        EnvironmentContext.distributor = distributor;
    }

    public static void distribute(CmdProtocol resp) {
        if (null != distributor) {
            distributor.write(resp);
        }
    }

    /**
     * 用于AgentServiceImpl中的消息发布，使用反射未直接调用
     * @param name Action name
     * @param param Action param
     * @param sessionId session id
     */
    public static void distributeAction(String name, String param, String sessionId) {
        CommandResponse response = new CommandResponse();
        response.setResponseType(ResponseType.ACTION);
        response.setSuccess(true);
        HashMap<String, String> body = new HashMap<>(2);
        body.put(CommandConst.ACTION_PROP_NAME_KEY, name);
        if (null != param && !param.isEmpty()) {
            body.put(CommandConst.ACTION_PROP_PARAM_KEY, param);
        }
        response.setBody(JsonUtils.toJSONString(body));
        response.setSessionId(sessionId);
        distribute(response);
    }
}
