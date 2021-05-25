package com.mz.jarboot.base;

import com.alibaba.fastjson.JSON;
import com.mz.jarboot.common.Command;
import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.MzException;
import com.mz.jarboot.common.ResultCodeConst;
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

    public CommandResponse sendCommand(Command command) {
        CommandResponse resp = new CommandResponse();
        sendText(JSON.toJSONString(command));
        if (Boolean.TRUE.equals(command.getAck())) {
            try {
                resp = respQueue.poll(CommonConst.MAX_RESPONSE_TIME, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                //ignore
                Thread.currentThread().interrupt();
            }
            if (null == resp) {
                resp = new CommandResponse();
                resp.setResultCode(ResultCodeConst.TIME_OUT);
                resp.setResultMsg("执行超时，" + command.getCmd());
            }
        }
        return resp;
    }

    public void sendCommand(String cmd, String param) {
        Command command = new Command();
        command.setCmd(cmd);
        command.setParam(param);
        command.setAck(false);
        sendCommand(command);
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
