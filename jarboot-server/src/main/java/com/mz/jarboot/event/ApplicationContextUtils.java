package com.mz.jarboot.event;

import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.utils.TaskUtils;
import org.springframework.context.ApplicationContext;

/**
 * @author majianzheng
 */
public class ApplicationContextUtils {
    private static ApplicationContext ctx;
    private ApplicationContextUtils() {}

    public static void publish(Object event) {
        if (null != ctx) {
            ctx.publishEvent(event);
        }
    }

    public static String getEnv(String name) {
        return ctx.getEnvironment().getProperty(name);
    }

    public static String getEnv(String name, String defaultValue) {
        return ctx.getEnvironment().getProperty(name, defaultValue);
    }

    public static ApplicationContext getContext() {
        return ctx;
    }

    public static void init(final ApplicationContext ctx) {
        ApplicationContextUtils.ctx = ctx;
        int maxStartTime = ctx.getEnvironment().getProperty("jarboot.services.max-start-time", int.class, 120000);
        TaskUtils.setMaxStartTime(maxStartTime);
        int maxExitTime = ctx.getEnvironment()
                .getProperty("jarboot.services.max-graceful-exit-time", int.class, CommonConst.MAX_WAIT_EXIT_TIME);
        AgentManager.getInstance().setMaxGracefulExitTime(maxExitTime);
    }
}
