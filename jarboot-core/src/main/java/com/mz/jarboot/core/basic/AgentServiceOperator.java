package com.mz.jarboot.core.basic;

import com.mz.jarboot.api.JarbootFactory;
import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.spi.CommandProcessor;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.common.*;
import com.mz.jarboot.common.notify.NotifyReactor;
import com.mz.jarboot.common.pojo.AgentClient;
import com.mz.jarboot.common.protocol.NotifyType;
import com.mz.jarboot.common.protocol.ResponseType;
import com.mz.jarboot.common.utils.ApiStringBuilder;
import com.mz.jarboot.core.cmd.CommandBuilder;
import com.mz.jarboot.core.event.ResponseEventBuilder;
import com.mz.jarboot.core.utils.HttpUtils;
import com.mz.jarboot.core.utils.LogUtils;
import com.mz.jarboot.core.utils.ThreadUtil;
import com.mz.jarboot.common.utils.StringUtils;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jianzhengma
 */
@SuppressWarnings({ "unused", "squid:S1181", "unchecked", "ConstantConditions" })
public class AgentServiceOperator {
    private static final Logger logger = LogUtils.getLogger();
    private static final String SET_STARTED_API = CommonConst.AGENT_CLIENT_CONTEXT + "/setStarted";
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    public static void setStarted() {
        if (STARTED.compareAndSet(false, true)) {
            AgentClient clientData = EnvironmentContext.getAgentClient();
            final String url = new ApiStringBuilder(SET_STARTED_API)
                    .add(CommonConst.SERVICE_NAME_PARAM, clientData.getServiceName())
                    .add(CommonConst.SID_PARAM, clientData.getSid())
                    .add(CommonConst.USER_DIR, clientData.getUserDir())
                    .build();
            HttpUtils.getSimple(url);
        }
        LogUtils.offlineDevLog("started check alive");
        ThreadUtil.checkAliveAndAutoExit();
    }

    public static String getServiceName() {
        return EnvironmentContext.getAgentClient().getServiceName();
    }

    public static void noticeInfo(String message, String sessionId) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        publish(NotifyType.INFO, message, sessionId);
    }

    public static void noticeWarn(String message, String sessionId) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        publish(NotifyType.WARN, message, sessionId);
    }

    public static void noticeError(String message, String sessionId) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        publish(NotifyType.ERROR, message, sessionId);
    }

    /**
     * 初始化Spring容器中的{@link CommandProcessor}的bean<br>
     * 前置条件：引入了spring-boot-starter-jarboot的依赖
     */
    public static void springContextInit() {
        Object context = JarbootFactory.getSpringApplicationContext();
        if (null == context) {
            AnsiLog.info("Current is not spring-boot application.");
            return;
        }
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

    private static void publish(NotifyType name, String param, String sessionId) {
        String bodyData = name.body(param);
        NotifyReactor
                .getInstance()
                .publishEvent(new ResponseEventBuilder()
                        .type(ResponseType.NOTIFY)
                        .success(true)
                        .body(bodyData)
                        .session(sessionId)
                        .build());
    }

    private AgentServiceOperator() {}
}
