package com.mz.jarboot.base;

import com.mz.jarboot.common.*;
import com.mz.jarboot.constant.CommonConst;
import javax.websocket.Session;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class AgentClient {
    private final Session session;
    private final String name;
    private final ArrayBlockingQueue<CommandResponse> respQueue = new ArrayBlockingQueue<>(16);
    private ClientState state;
    public AgentClient(String name, final Session session) {
        this.session = session;
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
     * 同步方式执行命令
     * @param command 命令
     * @return 命令结果
     */
    public CommandResponse sendInternalCommand(String command, String sessionId) {
        CommandResponse resp = new CommandResponse();
        CommandRequest request = new CommandRequest();
        request.setCommandType(CommandType.INTERNAL);
        request.setCommandLine(command);
        request.setSessionId(sessionId);
        sendText(request.toRaw());
        try {
            resp = respQueue.poll(CommonConst.MAX_RESPONSE_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //ignore
            Thread.currentThread().interrupt();
        }
        if (null == resp) {
            resp = new CommandResponse();
            resp.setSuccess(false);
            resp.setBody("执行超时");
        }
        return resp;
    }

    public void sendCommand(String command, String sessionId) {
        CommandRequest request = new CommandRequest();
        request.setCommandType(CommandType.USER_PUBLIC);
        request.setCommandLine(command);
        request.setSessionId(sessionId);
        sendText(request.toRaw());
    }

    public void onAck(CommandResponse resp) {
        respQueue.clear();//清空未处理等消息
        if (!respQueue.offer(resp)) {
            respQueue.clear();
            throw new MzException(ResultCodeConst.INTERNAL_ERROR, "offer failed.");
        }
    }

    private void sendText(String text) {
        try {
            session.getBasicRemote().sendText(text);
        } catch (IOException e) {
            //ignore
        }
    }
}
