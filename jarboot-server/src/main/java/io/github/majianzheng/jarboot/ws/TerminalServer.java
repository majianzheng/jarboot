package io.github.majianzheng.jarboot.ws;

import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.config.WsConfigurator;
import io.github.majianzheng.jarboot.terminal.TerminalProcess;
import io.github.majianzheng.jarboot.utils.CommonUtils;
import io.github.majianzheng.jarboot.utils.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mazheng
 */
@ServerEndpoint(value = "/jarboot/main/terminal/ws", configurator = WsConfigurator.class)
@RestController
@SuppressWarnings({"java:S1181"})
public class TerminalServer {
    private static final Logger logger = LoggerFactory.getLogger(TerminalServer.class);
    private static final Map<String, SessionProxy> SESSION_PROXY_MAP = new ConcurrentHashMap<>(16);
    private final TerminalProcess terminal = new TerminalProcess();
    @OnOpen
    public void onOpen(Session session) {
        String clusterHost = CommonUtils.getSessionClusterHost(session);
        if (CommonUtils.needProxy(clusterHost)) {
            SESSION_PROXY_MAP.put(clusterHost, new SessionProxy(session, clusterHost));
        } else {
            terminal.init(session);
        }
    }
    @OnClose
    public void onClose(Session session) {
        String clusterHost = CommonUtils.getSessionClusterHost(session);
        if (CommonUtils.needProxy(clusterHost)) {
            SessionProxy sessionProxy = SESSION_PROXY_MAP.remove(clusterHost);
            if (null != sessionProxy) {
                sessionProxy.proxyOnClose();
            }
        } else {
            try {
                terminal.destroy();
            } catch (Throwable e) {
                logger.debug(e.getMessage(), e);
            }
        }
        logger.info("销毁终端session:{}", session.getId());
    }
    @OnError
    public void onError(Throwable error, Session session) {
        logger.debug(error.getMessage(), error);
        if (error instanceof UnsupportedClassVersionError) {
            MessageUtils.warn("终端功能需要JDK 11或以上版本！");
        } else {
            MessageUtils.warn("终端启动失败：" + error.getMessage());
        }
        String clusterHost = CommonUtils.getSessionClusterHost(session);
        SessionProxy sessionProxy = SESSION_PROXY_MAP.remove(clusterHost);
        if (null != sessionProxy) {
            sessionProxy.proxyOnClose();
        }
        if (terminal.isAlive()) {
            terminal.destroy();
        }
    }

    @OnMessage
    public void onTextMessage(String message, Session session) {
        String clusterHost = CommonUtils.getSessionClusterHost(session);
        if (CommonUtils.needProxy(clusterHost)) {
            SESSION_PROXY_MAP.get(clusterHost).proxyOnText(message);
        } else {
            terminal.exec(message);
        }
    }

    @OnMessage
    public void onBinaryMessage(byte[] message, Session session) {
        String clusterHost = CommonUtils.getSessionClusterHost(session);
        if (CommonUtils.needProxy(clusterHost)) {
            SESSION_PROXY_MAP.get(clusterHost).proxyBinary(message);
        } else {
            TermSize size = JsonUtils.readValue(message, TermSize.class);
            if (null == size) {
                String msg = new String(message, StandardCharsets.UTF_8);
                logger.error("session({}), 解析窗口大小失败,msg:{}", session.getId(), msg);
                return;
            }
            logger.info("session({}), 更新窗口大小，col:{}, row:{}", session.getId(), size.getCol(), size.getRow());
            terminal.setWinSize(size.getCol(), size.getRow());
        }
    }

    public static class TermSize {
        private Integer col;
        private Integer row;

        public Integer getCol() {
            return col;
        }

        public void setCol(Integer col) {
            this.col = col;
        }

        public Integer getRow() {
            return row;
        }

        public void setRow(Integer row) {
            this.row = row;
        }
    }
}
