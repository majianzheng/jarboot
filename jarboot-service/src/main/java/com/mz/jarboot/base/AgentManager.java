package com.mz.jarboot.base;

import com.google.common.base.Stopwatch;
import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.ResponseType;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.event.AgentOfflineEvent;
import com.mz.jarboot.event.ApplicationContextUtils;
import com.mz.jarboot.task.TaskStatus;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.websocket.Session;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AgentManager {
    private static volatile AgentManager instance = null; //NOSONAR
    private final Map<String, AgentClient> clientMap = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private AgentManager(){}
    public static AgentManager getInstance() {
        if (null == instance) {
            synchronized (AgentManager.class) {
                if (null == instance) {
                    instance = new AgentManager();
                }
            }
        }
        return instance;
    }

    public void online(String server, Session session) {
        //目标进程上线
        clientMap.put(server, new AgentClient(server, session));
    }

    public void offline(String server) {
        final AgentClient client = clientMap.getOrDefault(server, null);
        if (null == client) {
            return;
        }
        WebSocketManager.getInstance().sendConsole(server, server + "下线！");
        synchronized (client) {// NOSONAR
            if (ClientState.EXITING.equals(client.getState())) {
                logger.info("目标进程已退出，唤醒killServer方法的执行线程");
                //发送了退出执行，唤醒killClient线程
                client.notify(); //NOSONAR
                clientMap.remove(server);
            } else {
                //先移除，防止再次点击终止时，会去执行已经关闭的会话
                clientMap.remove(server);
                //此时属于异常退出，发布异常退出事件，通知任务守护服务
                ApplicationContextUtils.publish(new AgentOfflineEvent(server));
                client.setState(ClientState.OFFLINE);
            }
        }
        WebSocketManager.getInstance().publishStatus(server, TaskStatus.STOPPED);
    }

    public boolean isOnline(String server) {
        final AgentClient client = clientMap.getOrDefault(server, null);
        if (null == client) {
            return false;
        }
        synchronized (client) {// NOSONAR
            return ClientState.ONLINE.equals(client.getState());
        }
    }

    public boolean killClient(String server) {
        final AgentClient client = clientMap.getOrDefault(server, null);
        if (null == client) {
            logger.debug("服务已经是退出状态，{}", server);
            return false;
        }
        synchronized (client) {// NOSONAR
            Stopwatch stopwatch = Stopwatch.createStarted();
            client.setState(ClientState.EXITING);
            sendInternalCommand(server, CommandConst.EXIT_CMD, CommandConst.SESSION_COMMON);
            //等目标进程发送offline信息时执行notify唤醒当前线程
            try {
                client.wait(CommonConst.MAX_WAIT_EXIT_TIME);//NOSONAR
            } catch (InterruptedException e) {
                //ignore
                Thread.currentThread().interrupt();
            }
            long costTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            if (clientMap.containsKey(server)) {
                logger.warn("未能成功退出！{}, 耗时:{}", server, costTime);
                //失败
                return false;
            } else {
                logger.debug("等待目标进程退出完成,耗时:{} ms", costTime);
                client.setState(ClientState.OFFLINE);
                WebSocketManager.getInstance().sendConsole(server, "进程优雅退出成功！");
            }
        }
        return true;
    }

    public void sendCommand(String server, String command, String sessionId) {
        if (StringUtils.isEmpty(server) || StringUtils.isEmpty(command)) {
            return;
        }
        AgentClient client = clientMap.getOrDefault(server, null);
        if (null == client) {
            WebSocketManager.getInstance().commandEnd(server, "未在线，无法执行命令", sessionId);
        } else {
            client.sendCommand(command, sessionId);
        }
    }

    public CommandResponse sendInternalCommand(String server, String command, String sessionId) {
        if (StringUtils.isEmpty(server) || StringUtils.isEmpty(command)) {
            WebSocketManager.getInstance().commandEnd(server, "", sessionId);
            return new CommandResponse();
        }
        AgentClient client = clientMap.getOrDefault(server, null);
        if (null == client) {
            CommandResponse resp = new CommandResponse();
            resp.setSuccess(false);
            WebSocketManager.getInstance().commandEnd(server, "", sessionId);
            return resp;
        }
        return client.sendInternalCommand(command, sessionId);
    }

    public void handleAgentResponse(String server, CommandResponse resp) {
        ResponseType type = resp.getResponseType();
        String sessionId = resp.getSessionId();
        switch (type) {
            case ACK:
                AgentManager.getInstance().onAck(server, resp);
                break;
            case CONSOLE:
                WebSocketManager.getInstance().sendConsole(server, resp.getBody(), sessionId);
                break;
            case JSON_RESULT:
                WebSocketManager.getInstance().renderJson(server, resp.getBody(), sessionId);
                break;
            case COMMAND_END:
                String msg = resp.getBody();
                if (StringUtils.isNotEmpty(msg) && Boolean.FALSE.equals(resp.getSuccess())) {
                    msg = String.format("<span style=\"color:red\">%s</span>", resp.getBody());
                }
                WebSocketManager.getInstance().commandEnd(server, msg, sessionId);
                break;
            default:
                //do nothing
                break;
        }
    }

    public void onAck(String server, CommandResponse resp) {
        AgentClient client = clientMap.getOrDefault(server, null);
        if (null != client) {
            client.onAck(resp);
        }
    }

    public void releaseAgentSession(String sessionId) {
        //向所有在线的agent客户端发送会话失效命令
        clientMap.forEach((k, v) -> sendInternalCommand(k, CommandConst.CANCEL_CMD, sessionId));
    }
}
