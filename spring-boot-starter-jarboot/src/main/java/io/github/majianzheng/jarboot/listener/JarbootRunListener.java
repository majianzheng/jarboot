package io.github.majianzheng.jarboot.listener;

import io.github.majianzheng.jarboot.Constants;
import io.github.majianzheng.jarboot.api.AgentService;
import io.github.majianzheng.jarboot.api.JarbootFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * SpringBoot生命周期监控
 * @author majianzheng
 */
public class JarbootRunListener implements SpringApplicationRunListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private volatile boolean startByJarboot = false;

    /**
     * SpringApplicationRunListener default construct
     * @param app app
     * @param args args
     */
    public JarbootRunListener(SpringApplication app, String[] args) {
        //do nothing
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        JarbootFactory.setSpringApplicationContext(context);
        //先判定是否使用了Jarboot启动
        try {
            ClassLoader.getSystemClassLoader().loadClass(Constants.AGENT_CLASS);
            startByJarboot = true;
            logger.info("Jarboot is starting spring boot application...");
        } catch (Exception e) {
            //ignore
        }
    }

    @SuppressWarnings("java:S1874")
    @Override
    public void started(ConfigurableApplicationContext context) {
        setStarted();
    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {
        if (!startByJarboot) {
            return;
        }
        try {
            JarbootFactory
                    .createAgentService()
                    .noticeError("Start Spring boot application failed.\n" +
                            exception.getMessage(), null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void setStarted() {
        if (startByJarboot) {
            logger.info("\u001B[1;92mSpring boot application is running with jarboot\u001B[0m \u001B[5m✨ \u001B[0m");
            try {
                AgentService agentService = JarbootFactory.createAgentService();
                agentService.setStarted();
                //初始化Spring
                agentService.getClass()
                        .getMethod(Constants.SPRING_INIT_METHOD)
                        .invoke(null);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            logger.info("Current application is not started by jarboot.");
        }
    }
}
