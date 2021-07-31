package com.mz.jarboot.spring.listener;

import com.mz.jarboot.api.JarbootFactory;
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

    public JarbootRunListener(SpringApplication sa, String[] args) {
        this.application = sa;
        this.args = args;
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        logger.info("Jarboot is starting spring boot application...");
    }

    @Override
    public void running(ConfigurableApplicationContext context) {
        logger.info("Spring boot application is running with jarboot.");
        try {
            JarbootFactory.createAgentService().setStarted();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {
        logger.error("Spring boot running error.", exception);
    }
}
