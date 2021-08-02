package com.mz.jarboot.listener;

import com.mz.jarboot.api.AgentService;
import com.mz.jarboot.api.JarbootFactory;
import com.mz.jarboot.common.CommandConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * SpringBoot生命周期监控
 * @author majianzheng
 */
@SuppressWarnings("all")
public class JarbootRunListener implements SpringApplicationRunListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private SpringApplication application;
    private String[] args;
    private volatile boolean startByJarboot = false;

    public JarbootRunListener(SpringApplication sa, String[] args) {
        this.application = sa;
        this.args = args;
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        //先判定是否使用了Jarboot启动
        try {
            Class<?> cls = ClassLoader.getSystemClassLoader().loadClass("com.mz.jarboot.agent.JarbootAgent");
            startByJarboot = true;
            logger.info("Jarboot is starting spring boot application...");
        } catch (Throwable e) {
            //ignore
        }
    }

    @Override
    public void running(ConfigurableApplicationContext context) {
        if (startByJarboot) {
            logger.info("Spring boot application is running with jarboot.");
            try {
                AgentService agentService = JarbootFactory.createAgentService();
                agentService.setStarted();
                //初始化Spring
                agentService.getClass()
                        .getMethod("springContextInit", Object.class)
                        .invoke(null, context);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            logger.info("Current application is not started by jarboot.");
        }
    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {
        if (!startByJarboot) {
            return;
        }
        try {
            JarbootFactory
                    .createAgentService()
                    .notice("Start Spring boot application failed.\n" + exception.getMessage(),
                            CommandConst.ACTION_NOTICE_ERROR);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
        boolean v = context.getEnvironment().getProperty("spring.jarboot.failed-auto-exit", boolean.class, true);
        if (v) {
            logger.error(exception.getMessage(), exception);
            //启动失败自动退出
            System.exit(-1);
        }
    }
}
