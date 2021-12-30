package com.mz.jarboot.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.protocol.CommandConst;
import com.mz.jarboot.common.utils.JsonUtils;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.event.ApplicationContextUtils;
import com.mz.jarboot.event.AttachStatus;
import com.mz.jarboot.security.JwtTokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;

/**
 * 向浏览器推送消息
 * @author majianzheng
 */
@ServerEndpoint("/jarboot/public/service/ws")
@RestController
public class WebSocketMainServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketMainServer.class);

    /** json请求中server */
    private static final String SERVER_KEY = "server";
    /** json请求中func */
    private static final String FUNC_KEY = "func";
    /** json请求中body */
    private static final String BODY_KEY = "body";
    /** json请求中sid */
    private static final String SID_KEY = "sid";
    /** 心跳ping */
    private static final String PING = "ping";

    private static class Holder {
        static final JwtTokenManager JWT_MGR = ApplicationContextUtils.getContext().getBean(JwtTokenManager.class);
    }

    private enum FuncCode {
        /** 执行命令func */
        CMD_FUNC,
        /** 取消执行命令func */
        CANCEL_FUNC,
        /** 信任主机func */
        TRUST_ONCE_FUNC,
        /** 检查是否信任主机func */
        CHECK_TRUSTED_FUNC,
        /** 断开诊断 */
        DETACH_FUNC,
    }

    /**
     * 连接建立成功调用的方法
     * */
    @OnOpen
    public void onOpen(Session session) {
        //获取token
        List<String> array = session.getRequestParameterMap().get("token");
        if (CollectionUtils.isEmpty(array)) {
            logger.error("WebSocket connect failed, need token!");
            try {
                session.getBasicRemote().sendText("Token is empty!");
                session.close();
            } catch (IOException exception) {
                logger.warn(exception.getMessage(), exception);
            }
            return;
        }
        String token = array.get(0);
        //校验token合法性
        try {
            Holder.JWT_MGR.validateToken(token);
        } catch (Exception e) {
            logger.error("Validate token failed!\ntoken:{}", token, e);
            try {
                session.getBasicRemote().sendText("Validate token failed!");
                session.close();
            } catch (IOException exception) {
                logger.warn(exception.getMessage(), exception);
            }
            return;
        }

        WebSocketManager.getInstance().newConnect(session);
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose( Session session) {
        AgentManager.getInstance().releaseAgentSession(session.getId());
        WebSocketManager.getInstance().delConnect(session.getId());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息*/
    @OnMessage
    public void onBinaryMessage(byte[] message, Session session) {
        //do nothing
    }

    @OnMessage
    public void onTextMessage(String message, Session session) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        if (PING.equals(message)) {
            return;
        }
        JsonNode json = JsonUtils.readAsJsonNode(message);
        if (null == json) {
            logger.error("解析json失败！{}", message);
            return;
        }
        final FuncCode[] values = FuncCode.values();
        int func = json.get(FUNC_KEY).asInt(-1);
        if (func < 0 || func >= values.length) {
            return;
        }
        final FuncCode funcCode = values[func];
        String sid = json.get(SID_KEY).asText(StringUtils.EMPTY);
        switch (funcCode) {
            case CMD_FUNC:
                String body = json.get(BODY_KEY).asText(StringUtils.EMPTY);
                String server = json.get(SERVER_KEY).asText(StringUtils.EMPTY);
                AgentManager.getInstance().sendCommand(server, sid, body, session.getId());
                break;
            case CANCEL_FUNC:
                AgentManager.getInstance().sendInternalCommand(sid, CommandConst.CANCEL_CMD, session.getId());
                break;
            case TRUST_ONCE_FUNC:
                AgentManager.getInstance().trustOnce(sid);
                break;
            case CHECK_TRUSTED_FUNC:
                if (AgentManager.getInstance().checkNotTrusted(sid)) {
                    WebSocketManager.getInstance().debugProcessEvent(sid, AttachStatus.NOT_TRUSTED);
                }
                break;
            case DETACH_FUNC:
                AgentManager.getInstance().sendInternalCommand(sid, CommandConst.SHUTDOWN, session.getId());
                break;
            default:
                logger.debug("Unknown func, func:{}", func);
                break;
        }
    }

    /**
     * 连接异常
     * @param session 会话
     * @param error 错误
     */
    @OnError
    public void onError(Session session, Throwable error) {
        logger.debug(error.getMessage(), error);
        this.onClose(session);
    }
}
