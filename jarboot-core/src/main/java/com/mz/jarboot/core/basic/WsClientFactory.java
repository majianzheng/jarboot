package com.mz.jarboot.core.basic;

import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.utils.HttpUtils;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

/**
 * WebSocket client factory for create socket client.
 * @author majianzheng
 */
public class WsClientFactory {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    @SuppressWarnings("all")
    private static volatile WsClientFactory instance = null;
    private okhttp3.WebSocket client = null;
    private String url = null;

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

        url = String.format("ws://%s/public/jarboot/agent/ws/%s",
                EnvironmentContext.getHost(), server);
        logger.debug("initClient {}", url);
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
    public synchronized void createSingletonClient(okhttp3.WebSocketListener handler) {
        try {
            client = HttpUtils.HTTP_CLIENT
                    .newWebSocket(new Request
                            .Builder()
                            .get()
                            .url(url)
                            .build(), handler);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public okhttp3.WebSocket getSingletonClient() {
        return this.client;
    }
}
