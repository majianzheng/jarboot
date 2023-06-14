package com.mz.jarboot.ws;

import com.mz.jarboot.common.utils.JsonUtils;
import com.mz.jarboot.terminal.TerminalProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.nio.charset.StandardCharsets;

/**
 * @author mazheng
 */
@ServerEndpoint("/jarboot/main/terminal/ws")
@RestController
public class TerminalServer {
    private static final Logger logger = LoggerFactory.getLogger(TerminalServer.class);
    private final TerminalProcess terminal = new TerminalProcess();
    @OnOpen
    public void onOpen(Session session) {
        terminal.init(session);
    }
    @OnClose
    public void onClose( Session session) {
        terminal.destroy();
        logger.info("销毁终端session:{}", session.getId());
    }
    @OnError
    public void onError(Session session, Throwable error) {
        logger.debug(error.getMessage(), error);
        this.onClose(session);
    }

    @OnMessage
    public void onTextMessage(String message, Session session) {
        terminal.exec(message);
    }

    @OnMessage
    public void onBinaryMessage(byte[] message, Session session) {
        TermSize size = JsonUtils.readValue(message, TermSize.class);
        if (null == size) {
            String msg = new String(message, StandardCharsets.UTF_8);
            logger.error("session({}), 解析窗口大小失败,msg:{}", session.getId(), msg);
            return;
        }
        logger.info("session({}), 更新窗口大小，col:{}, row:{}", session.getId(), size.getCol(), size.getRow());
        terminal.setWinSize(size.getCol(), size.getRow());
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