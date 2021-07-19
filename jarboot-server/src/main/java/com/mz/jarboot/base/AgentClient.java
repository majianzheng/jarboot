package com.mz.jarboot.base;

import com.mz.jarboot.common.*;
import com.mz.jarboot.ws.MessageQueueOperator;
import com.mz.jarboot.ws.WebSocketManager;
import javax.websocket.Session;

public final class AgentClient extends MessageQueueOperator {
    private final String name;
    private ClientState state;
    public AgentClient(String name, final Session session) {
        super(session);
        this.name = name;
        this.state = ClientState.ONLINE;
    }

    public String getName() {
        return this.name;
    }

    public void setState(ClientState state) {
        this.state = state;
    }

    public ClientState getState() {
        return this.state;
    }

    /**
     * 执行内部命令
     * @param command 命令
     */
    public void sendInternalCommand(String command, String sessionId) {
        CommandRequest request = new CommandRequest();
        request.setCommandType(CommandType.INTERNAL);
        request.setCommandLine(command);
        request.setSessionId(sessionId);
        WebSocketManager.messageProducer(this, request.toRaw());
    }

    public void sendCommand(String command, String sessionId) {
        CommandRequest request = new CommandRequest();
        request.setCommandType(CommandType.USER_PUBLIC);
        request.setCommandLine(command);
        request.setSessionId(sessionId);
        WebSocketManager.messageProducer(this, request.toRaw());
    }
}
