package com.mz.jarboot.core.basic;

import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.ws.MessageHandler;
import com.mz.jarboot.core.ws.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.UnsupportedEncodingException;

/**
 * Singleton core factory for create socket client, thread pool, strategy instance.
 * @author jianzhengma
 */
public class SingletonCoreFactory {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private static volatile SingletonCoreFactory instance = null; //NOSONAR
    private WebSocketClient client = null;
    private TemplateEngine engine = null;
    public static SingletonCoreFactory getInstance() {
        if (null == instance) {
            synchronized (SingletonCoreFactory.class) {
                if (null == instance) {
                    instance = new SingletonCoreFactory();
                }
            }
        }
        return instance;
    }
    public synchronized WebSocketClient createSingletonClient(MessageHandler handler) {
        if (null != client) {
            return client;
        }
        String server = EnvironmentContext.getServer();
        //服务目录名支持中文，检查到中文后进行编码
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("[\\u4e00-\\u9fa5]").matcher(server);
        while (matcher.find()) {
            String tmp = matcher.group();
            try {
                server = server.replaceAll(tmp, java.net.URLEncoder.encode(tmp, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage(), e);
                return null;
            }
        }

        String url = String.format("ws://%s/jarboot-agent/ws/%s",
                EnvironmentContext.getHost(), server);
        logger.debug("initClient {}", url);
        client = new WebSocketClient(url);
        boolean isOk = client.connect(handler);
        if (!isOk) {
            logger.warn("连接jarboot-server服务失败");
            client.disconnect();
            client = null;
            return null;
        }
        logger.info("createSingletonClient>>>>");
        return client;
    }

    public TemplateEngine createTemplateEngine() {
        if (null != engine) {
            return engine;
        }
        engine = new TemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setCharacterEncoding("UTF-8");
        engine.setTemplateResolver(resolver);
        return engine;
    }

    public WebSocketClient getSingletonClient() {
        return this.client;
    }
}
