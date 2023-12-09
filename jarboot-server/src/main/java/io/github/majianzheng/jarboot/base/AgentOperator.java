package io.github.majianzheng.jarboot.base;

import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;
import io.github.majianzheng.jarboot.common.notify.NotifyReactor;
import io.github.majianzheng.jarboot.common.protocol.CommandConst;
import io.github.majianzheng.jarboot.common.protocol.CommandRequest;
import io.github.majianzheng.jarboot.common.protocol.CommandType;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.event.SendCommandEvent;
import io.github.majianzheng.jarboot.ws.MessageSenderEvent;
import io.github.majianzheng.jarboot.ws.SessionOperator;

import javax.websocket.Session;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

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

    public AgentOperator(String userDir, String name, String sid, final Session session) {
        super(session);
        this.userDir = userDir;
        this.name = name;
        this.sid = sid;
        this.state = ClientState.STARTING;
        this.pid = StringUtils.EMPTY;
        this.trusted = false;
    }

    public String getUserDir() {
        return userDir;
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

    public void heartbeat(Set<String> sessionIds) {
        String cmd = CommandConst.HEARTBEAT;
        if (null != sessionIds && !sessionIds.isEmpty()) {
            String arg = Base64.getEncoder().encodeToString(String.join(",", sessionIds).getBytes(StandardCharsets.UTF_8));
            cmd = String.format("%s %s", CommandConst.HEARTBEAT, arg);
        }
        sendCommand(cmd, StringUtils.EMPTY, CommandType.INTERNAL, 1, 1);
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
