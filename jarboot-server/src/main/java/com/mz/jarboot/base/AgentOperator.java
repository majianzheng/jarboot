package com.mz.jarboot.base;

import com.mz.jarboot.api.pojo.ServiceSetting;
import com.mz.jarboot.common.notify.NotifyReactor;
import com.mz.jarboot.common.protocol.*;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.event.SendCommandEvent;
import com.mz.jarboot.ws.MessageSenderEvent;
import com.mz.jarboot.ws.SessionOperator;

import javax.websocket.Session;

/**
 * @author majianzheng
 */
public final class AgentOperator extends SessionOperator {
    private final String name;
    private final String sid;
    private ServiceSetting setting;
    private ClientState state;
    private String pid;
    private boolean trusted;

    public AgentOperator(String name, String sid, final Session session) {
        super(session);
        this.name = name;
        this.sid = sid;
        this.state = ClientState.STARTING;
        this.pid = StringUtils.EMPTY;
        this.trusted = false;
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

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public ServiceSetting getSetting() {
        return setting;
    }

    public void setSetting(ServiceSetting setting) {
        this.setting = setting;
    }

    /**
     * 执行内部命令
     * @param command 命令
     * @param sessionId 会话id
     */
    public void sendInternalCommand(String command, String sessionId) {
        sendCommand(command, sessionId, CommandType.INTERNAL, 1, 1);
    }

    /**
     * 发送命令
     * @param command 命令
     * @param sessionId 会话id
     */
    public void sendCommand(String command, String sessionId, int row, int col) {
        sendCommand(command, sessionId, CommandType.USER_PUBLIC, row, col);
    }

    public void heartbeat() {
        sendCommand(CommandConst.HEARTBEAT, StringUtils.EMPTY, CommandType.HEARTBEAT, 1, 1);
    }

    public boolean isTrusted() {
        return trusted;
    }

    public void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }

    private void sendCommand(String command, String sessionId, CommandType type, int row, int col) {
        CommandRequest request = new CommandRequest();
        request.setCommandType(type);
        request.setCommandLine(command);
        request.setSessionId(sessionId);
        request.setRow(row);
        request.setCol(col);
        newMessage(request.toRaw());
    }

    @Override
    protected void publish(MessageSenderEvent event) {
        NotifyReactor.getInstance().publishEvent(new SendCommandEvent(event));
    }
}
