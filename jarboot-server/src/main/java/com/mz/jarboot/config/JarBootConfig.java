package com.mz.jarboot.config;

import com.mz.jarboot.auth.annotation.Permission;
import com.mz.jarboot.base.PermissionsCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Map;

/**
 * jarboot配置类
 * @author majianzheng
 */
@Configuration
public class JarBootConfig {
    private static final int MAX_BUFFER_SIZE = 16384;
    @Autowired
    private ApplicationContext ctx;
    @Autowired
    private PermissionsCache permissionsCache;

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
        Map<String, Object> controllers = ctx.getBeansWithAnnotation(Permission.class);
        HashSet<Class<?>> classes = new HashSet<>();
        controllers.forEach((k, v) -> classes.add(v.getClass()));
        permissionsCache.initClassMethod(classes);
    }
}
