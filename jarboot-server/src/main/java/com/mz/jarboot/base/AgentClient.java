package com.mz.jarboot.base;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.common.*;
import com.mz.jarboot.ws.MessageQueueOperator;
import javax.websocket.Session;

/**
 * @author majianzheng
 */
public final class AgentClient extends MessageQueueOperator {
    private final String name;
    private final String sid;
    private ClientState state;
    private int pid;
    public AgentClient(String name, String sid, final Session session) {
        super(session);
        this.name = name;
        this.sid = sid;
        this.state = ClientState.STARTING;
        this.pid = CommonConst.INVALID_PID;
    }

    public String getName() {
        return this.name;
    }

    public String getSid() {
        return this.sid;
    }

    public void setState(ClientState state) {
        this.state = state;
    }

    public ClientState getState() {
        return this.state;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
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
        this.newMessage(request.toRaw());
    }

    public void sendCommand(String command, String sessionId) {
        CommandRequest request = new CommandRequest();
        request.setCommandType(CommandType.USER_PUBLIC);
        request.setCommandLine(command);
        request.setSessionId(sessionId);
        this.newMessage(request.toRaw());
    }
}
