package com.mz.jarboot.event;

import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.service.TaskWatchService;
import com.mz.jarboot.utils.TaskUtils;
import org.springframework.context.ApplicationContext;

/**
 * ApplicationContext工具类
 * @author majianzheng
 */
public class ApplicationContextUtils {
    /** Spring ApplicationContext */
    private static ApplicationContext ctx;

    /**
     * 发布Application事件
     * @param event 事件
     */
    public static void publish(Object event) {
        if (null != ctx) {
            ctx.publishEvent(event);
        }
    }

    /**
     * 获取配置
     * @param name 配置名
     * @return 配置值
     */
    public static String getEnv(String name) {
        return ctx.getEnvironment().getProperty(name);
    }

    /**
     * 获取配置
     * @param name 配置名
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static String getEnv(String name, String defaultValue) {
        return ctx.getEnvironment().getProperty(name, defaultValue);
    }

    /**
     * 获取{@link ApplicationContext}
     * @return {@link ApplicationContext}
     */
    public static ApplicationContext getContext() {
        return ctx;
    }

    /**
     * 初始化
     * @param ctx {@link ApplicationContext}
     */
    public static void init(final ApplicationContext ctx) {
        ApplicationContextUtils.ctx = ctx;
        int maxStartTime = ctx
                .getEnvironment()
                .getProperty("jarboot.services.max-start-time", int.class, 120000);
        TaskUtils.setMaxStartTime(maxStartTime);

        int maxExitTime = ctx
                .getEnvironment()
                .getProperty("jarboot.services.max-graceful-exit-time", int.class, CommonConst.MAX_WAIT_EXIT_TIME);
        AgentManager.getInstance().setMaxGracefulExitTime(maxExitTime);

        //启动TaskWatchService
        TaskWatchService taskWatchService = ctx.getBean(TaskWatchService.class);
        taskWatchService.init();
    }

    private ApplicationContextUtils() {}
}
