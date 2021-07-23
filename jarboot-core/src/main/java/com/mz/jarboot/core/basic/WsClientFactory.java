package com.mz.jarboot.core.basic;

import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.ws.MessageHandler;
import com.mz.jarboot.core.ws.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

/**
 * WebSocket client factory for create socket client.
 * @author jianzhengma
 */
public class WsClientFactory {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    @SuppressWarnings("all")
    private static volatile WsClientFactory instance = null;
    private WebSocketClient client = null;

    private WsClientFactory() {
        String server = EnvironmentContext.getServer();
        //服务目录名支持中文，检查到中文后进行编码
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("[\\u4e00-\\u9fa5]").matcher(server);
        while (matcher.find()) {
            String tmp = matcher.group();
            try {
                server = server.replaceAll(tmp, java.net.URLEncoder.encode(tmp, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage(), e);
                return;
            }
        }

        String url = String.format("ws://%s/public/jarboot-agent/ws/%s",
                EnvironmentContext.getHost(), server);
        logger.debug("initClient {}", url);
        client = new WebSocketClient(url);
    }

    public static WsClientFactory getInstance() {
        if (null == instance) {
            synchronized (WsClientFactory.class) {
                if (null == instance) {
                    instance = new WsClientFactory();
                }
            }
        }
        return instance;
    }
    public synchronized WebSocketClient createSingletonClient(MessageHandler handler) {
        boolean isOk = client.connect(handler);
        if (!isOk) {
            logger.warn("连接jarboot-server服务失败");
            client.disconnect();
        }
        return client;
    }

    public WebSocketClient getSingletonClient() {
        return this.client;
    }
}
