package com.mz.jarboot.event;

import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.service.TaskWatchService;
import com.mz.jarboot.utils.SettingUtils;
import com.mz.jarboot.utils.TaskUtils;
import org.springframework.context.ApplicationContext;

/**
 * ApplicationContext工具类
 * @author majianzheng
 */
public class ApplicationContextUtils {
    /** Spring ApplicationContext */
    private static ApplicationContext applicationContext;

    /**
     * 获取{@link ApplicationContext}
     * @return {@link ApplicationContext}
     */
    public static ApplicationContext getContext() {
        return applicationContext;
    }

    /**
     * 初始化
     * @param ctx {@link ApplicationContext}
     */
    public static void init(final ApplicationContext ctx) {
        //最大启动超时时间配置
        ApplicationContextUtils.applicationContext = ctx;
        int maxStartTime = ctx
                .getEnvironment()
                .getProperty("jarboot.services.max-start-time", int.class, 120000);
        TaskUtils.setMaxStartTime(maxStartTime);

        //获取当前端口配置
        int port = ctx
                .getEnvironment()
                .getProperty(CommonConst.PORT_KEY, int.class, CommonConst.DEFAULT_PORT);
        SettingUtils.init(port);

        //最大优雅退出等待时间配置
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
