package com.mz.jarboot.core.basic;

import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.spi.CommandProcessor;
import com.mz.jarboot.common.*;
import com.mz.jarboot.common.protocol.CommandConst;
import com.mz.jarboot.common.protocol.CommandResponse;
import com.mz.jarboot.common.protocol.ResponseType;
import com.mz.jarboot.common.utils.JsonUtils;
import com.mz.jarboot.core.cmd.CommandBuilder;
import com.mz.jarboot.core.stream.ResultStreamDistributor;
import com.mz.jarboot.core.utils.HttpUtils;
import com.mz.jarboot.core.utils.LogUtils;
import com.mz.jarboot.core.utils.StringUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jianzhengma
 */
public class AgentServiceOperator {
    private static final Logger logger = LogUtils.getLogger();
    private static final String SET_STARTED_API = "/api/jarboot/public/agent/setStarted?server=";
    private static volatile boolean started = false;

    public static void setStarted() {
        if (started) {
            return;
        }
        AgentClientPojo clientData = EnvironmentContext.getClientData();
        HttpUtils.getSimple(SET_STARTED_API + clientData.getServer() +
                "&sid=" + clientData.getSid());
        started = true;
    }

    public static String getServer() {
        return EnvironmentContext.getClientData().getServer();
    }

    public static void restartSelf() {
        action(CommandConst.ACTION_RESTART, null, CommandConst.SESSION_COMMON);
    }

    public static void noticeInfo(String message, String sessionId) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        action(CommandConst.ACTION_NOTICE_INFO, message, sessionId);
    }

    public static void noticeWarn(String message, String sessionId) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        action(CommandConst.ACTION_NOTICE_WARN, message, sessionId);
    }

    public static void noticeError(String message, String sessionId) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        action(CommandConst.ACTION_NOTICE_ERROR, message, sessionId);
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

    private static void action(String name, String param, String sessionId) {
        if (StringUtils.isEmpty(sessionId)) {
            sessionId = CommandConst.SESSION_COMMON;
        }
        try {
            distributeAction(name, param, sessionId);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static void distributeAction(String name, String param, String sessionId) {
        CommandResponse response = new CommandResponse();
        response.setResponseType(ResponseType.ACTION);
        response.setSuccess(true);
        HashMap<String, String> body = new HashMap<>(2);
        body.put(CommandConst.ACTION_PROP_NAME_KEY, name);
        if (null != param && !param.isEmpty()) {
            body.put(CommandConst.ACTION_PROP_PARAM_KEY, param);
        }
        String bodyData = JsonUtils.toJsonString(body);
        response.setBody(bodyData);
        response.setSessionId(sessionId);
        ResultStreamDistributor.write(response);
    }

    private AgentServiceOperator() {}
}
