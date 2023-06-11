package com.mz.jarboot.config;

import com.mz.jarboot.common.notify.NotifyReactor;
import com.mz.jarboot.ws.MessageSenderSubscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import javax.annotation.PostConstruct;

/**
 * jarboot配置类
 * @author majianzheng
 */
@Configuration
public class JarBootConfig {
    private static final int MAX_BUFFER_SIZE = 4096;
    @Autowired
    private ApplicationContext ctx;

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxBinaryMessageBufferSize(MAX_BUFFER_SIZE);
        container.setMaxTextMessageBufferSize(MAX_BUFFER_SIZE);
        return container;
    }

    @PostConstruct
    public void init() {
        //注册消息发送订阅
        NotifyReactor.getInstance().registerSubscriber(new MessageSenderSubscriber());
    }
}
