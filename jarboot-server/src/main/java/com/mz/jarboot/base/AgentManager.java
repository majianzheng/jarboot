package com.mz.jarboot.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.mz.jarboot.api.pojo.ServerSetting;
import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.JsonUtils;
import com.mz.jarboot.common.ResponseType;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.event.*;
import com.mz.jarboot.task.TaskStatus;
import com.mz.jarboot.utils.PropertyFileUtils;
import com.mz.jarboot.utils.TaskUtils;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.websocket.Session;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author majianzheng
 */
@SuppressWarnings("all")
public class AgentManager {
    private static volatile AgentManager instance = null;
    private final ConcurrentHashMap<String, AgentClient> clientMap = new ConcurrentHashMap<>(16);
    private final ConcurrentHashMap<String, CountDownLatch> startingLatchMap = new ConcurrentHashMap<>(16);
    private final ConcurrentHashMap<Integer, String> serverPid = new ConcurrentHashMap<>(16);
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private int maxGracefulExitTime = CommonConst.MAX_WAIT_EXIT_TIME;

    private AgentManager(){}

    public static AgentManager getInstance() {
        return AgentManagerHolder.instance;
    }

    private static class AgentManagerHolder {
        static AgentManager instance = new AgentManager();
    }

    public void online(String server, Session session, String sid) {
        //目标进程上线
        AgentClient client = new AgentClient(server, sid, session);
        clientMap.put(sid, client);
        WebSocketManager.getInstance().publishStatus(sid, TaskStatus.ONLINE);
        CountDownLatch latch = startingLatchMap.getOrDefault(sid, null);
        if (null == latch) {
            client.setState(ClientState.ONLINE);
        } else {
            latch.countDown();
        }
        int pid = TaskUtils.getPid(server, sid);
        if (pid > 0) {
            //属于受管理的服务
            serverPid.put(pid, sid);
            client.setPid(pid);
        }
    }

    public void offline(String server, String sid) {
        final AgentClient client = clientMap.getOrDefault(sid, null);
        if (null == client) {
            return;
        }
        WebSocketManager.getInstance().publishStatus(sid, TaskStatus.OFFLINE);
        int pid = client.getPid();
        if (pid > 0) {
            serverPid.remove(pid);
        }
        WebSocketManager.getInstance().sendConsole(sid, server + "下线！");
        synchronized (client) {
            //同时判定STARTING，因为启动可能会失败，需要唤醒等待启动完成的线程
            if (ClientState.EXITING.equals(client.getState()) || ClientState.STARTING.equals(client.getState())) {
                //发送了退出执行，唤醒killClient或waitServerStarted线程
                client.notify();
                clientMap.remove(sid);
            } else {
                logger.warn("{} is offlined!", sid);
                //先移除，防止再次点击终止时，会去执行已经关闭的会话
                clientMap.remove(sid);
                //此时属于异常退出，发布异常退出事件，通知任务守护服务
                TaskEvent event = new TaskEvent(TaskEventEnum.OFFLINE, server, sid);
                ApplicationContextUtils.publish(event);
                client.setState(ClientState.OFFLINE);
            }
        }
    }

    public boolean isOnline(String sid) {
        final AgentClient client = clientMap.getOrDefault(sid, null);
        if (null == client) {
            return false;
        }
        if (!client.isOpen()) {
            logger.warn("{} websocket session 已经关闭！", sid);
            clientMap.remove(sid);
            return false;
        }
        synchronized (client) {
            return ClientState.ONLINE.equals(client.getState());
        }
    }

    public boolean killClient(String server, String sid) {
        final AgentClient client = clientMap.getOrDefault(sid, null);
        if (null == client) {
            logger.debug("服务已经是退出状态，{}", server);
            return false;
        }
        synchronized (client) {
            long startTime = System.currentTimeMillis();
            client.setState(ClientState.EXITING);
            sendInternalCommand(sid, CommandConst.EXIT_CMD, CommandConst.SESSION_COMMON);
            //等目标进程发送offline信息时执行notify唤醒当前线程
            try {
                client.wait(maxGracefulExitTime);
            } catch (InterruptedException e) {
                //ignore
                Thread.currentThread().interrupt();
            }
            long costTime = System.currentTimeMillis() - startTime;
            if (clientMap.containsKey(sid)) {
                logger.warn("未能成功退出！{}, 耗时:{}", server, costTime);
                //失败
                return false;
            } else {
                logger.debug("等待目标进程退出完成,耗时:{} ms", costTime);
                client.setState(ClientState.OFFLINE);
                WebSocketManager.getInstance().sendConsole(sid, "进程优雅退出成功！");
            }
        }
        return true;
    }

    public void sendCommand(String server, String sid, String command, String sessionId) {
        if (StringUtils.isEmpty(sid) || StringUtils.isEmpty(command)) {
            return;
        }
        AgentClient client = clientMap.getOrDefault(sid, null);
        if (null == client) {
            if (isOnline(sid)) {
                //如果进程仍然存活，尝试使用attach重新连接
                tryReConnect(server, sid, sessionId);

                client = clientMap.getOrDefault(sid, null);
                if (null == client) {
                    WebSocketManager.getInstance().commandEnd(sid, "连接断开，重连失败，请稍后重试", sessionId);
                } else {
                    client.sendCommand(command, sessionId);
                }
            } else {
                //未在线，进程不存在
                WebSocketManager.getInstance().commandEnd(sid, "未在线，无法执行命令", sessionId);
            }
        } else {
            client.sendCommand(command, sessionId);
        }
    }

    private void tryReConnect(String server, String sid, String sessionId) {
        CountDownLatch latch = startingLatchMap.computeIfAbsent(sid, k -> new CountDownLatch(1));
        try {
            TaskUtils.attach(server, sid);
            WebSocketManager.getInstance().sendConsole(sid, "连接断开，重连中...", sessionId);
            if (!latch.await(CommonConst.MAX_AGENT_CONNECT_TIME, TimeUnit.SECONDS)) {
                logger.error("Attach and wait server connect timeout，{}", server);
                WebSocketManager.getInstance().sendConsole(sid, "Attach重连超时！", sessionId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            startingLatchMap.remove(sid);
        }
    }

    public void sendInternalCommand(String sid, String command, String sessionId) {
        if (StringUtils.isEmpty(sid) || StringUtils.isEmpty(command)) {
            WebSocketManager.getInstance().commandEnd(sid, StringUtils.EMPTY, sessionId);
            return;
        }
        AgentClient client = clientMap.getOrDefault(sid, null);
        if (null == client) {
            WebSocketManager.getInstance().commandEnd(sid, StringUtils.EMPTY, sessionId);
            return;
        }
        client.sendInternalCommand(command, sessionId);
    }

    public void handleAgentResponse(String server, String sid, CommandResponse resp, Session session) {
        ResponseType type = resp.getResponseType();
        String sessionId = resp.getSessionId();
        switch (type) {
            case HEARTBEAT:
                AgentClient client = clientMap.getOrDefault(sid, null);
                if (null == client || !ClientState.ONLINE.equals(client.getState())) {
                    this.online(server, session, sid);
                    this.onServerStarted(server, sid);
                    WebSocketManager.getInstance().sendConsole(sid, server + " reconnected by heartbeat!");
                    logger.info("reconnected by heartbeat {}, {}", server, sid);
                    WebSocketManager.getInstance().publishStatus(sid, TaskStatus.STARTED);
                }
                sendInternalCommand(sid, CommandConst.HEARTBEAT, CommandConst.SESSION_COMMON);
                break;
            case CONSOLE:
                WebSocketManager.getInstance().sendConsole(sid, resp.getBody(), sessionId);
                break;
            case BACKSPACE:
                WebSocketManager.getInstance().backspace(sid, resp.getBody(), sessionId);
                break;
            case BACKSPACE_LINE:
                WebSocketManager.getInstance().backspaceLine(sid, resp.getBody(), sessionId);
                break;
            case STD_PRINT:
                //启动中的控制台消息
                WebSocketManager.getInstance().sendPrint(sid, resp.getBody(), sessionId);
                break;
            case JSON_RESULT:
                WebSocketManager.getInstance().renderJson(sid, resp.getBody(), sessionId);
                break;
            case COMMAND_END:
                String msg = resp.getBody();
                if (StringUtils.isNotEmpty(msg) && Boolean.FALSE.equals(resp.getSuccess())) {
                    msg = String.format("<span style=\"color:red\">%s</span>", resp.getBody());
                }
                WebSocketManager.getInstance().commandEnd(sid, msg, sessionId);
                break;
            case ACTION:
                this.handleAction(resp.getBody(), sessionId, sid);
                break;
            default:
                //do nothing
                break;
        }
    }

    public void onServerStarted(final String server, final String sid) {
        AgentClient client = clientMap.getOrDefault(sid, null);
        if (null == client) {
            logger.error("Server {} in offline already!", server);
            WebSocketManager.getInstance().sendConsole(sid, server + " is offline now！");
            return;
        }
        synchronized (client) {
            if (ClientState.STARTING.equals(client.getState())) {
                //发送启动成功，唤醒waitServerStarted线程
                client.notify();
            }
            client.setState(ClientState.ONLINE);
        }
    }

    public void waitServerStarted(String server, String sid, int millis) {
        AgentClient client = clientMap.getOrDefault(sid, null);
        if (null == client) {
            CountDownLatch latch = startingLatchMap.computeIfAbsent(sid, k -> new CountDownLatch(1));
            try {
                if (!latch.await(CommonConst.MAX_AGENT_CONNECT_TIME, TimeUnit.SECONDS)) {
                    logger.error("Wait server connect timeout，{}", server);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                startingLatchMap.remove(sid);
            }
            client = clientMap.getOrDefault(sid, null);
            if (null == client) {
                WebSocketManager.getInstance().sendConsole(sid, server + " connect timeout！");
                return;
            }
        }

        synchronized (client) {
            if (!ClientState.STARTING.equals(client.getState())) {
                logger.info("Current server({}) is not starting now, wait server started error. statue:{}",
                        server, client.getState());
                WebSocketManager.getInstance().sendConsole(sid,
                        server + " is not starting, wait started error. status:" + client.getState());
                return;
            }
            try {
                client.wait(millis);
            } catch (InterruptedException e) {
                //ignore
                Thread.currentThread().interrupt();
            }
        }
    }

    public void releaseAgentSession(String sessionId) {
        //向所有在线的agent客户端发送会话失效命令
        clientMap.forEach((k, v) -> sendInternalCommand(v.getSid(), CommandConst.CANCEL_CMD, sessionId));
    }

    public void setMaxGracefulExitTime(int d) {
        this.maxGracefulExitTime = d;
    }

    public int getMaxGracefulExitTime() {
        return this.maxGracefulExitTime;
    }

    public boolean isManageredServer(int pid) {
        return serverPid.containsKey(pid);
    }

    private void handleAction(String data, String sessionId, String sid) {
        JsonNode body = JsonUtils.readAsJsonNode(data);
        if (null == body || !body.isObject() || !body.has(CommandConst.ACTION_PROP_NAME_KEY)) {
            return;
        }
        String action = body.get(CommandConst.ACTION_PROP_NAME_KEY).asText(StringUtils.EMPTY);
        String param = body.get(CommandConst.ACTION_PROP_PARAM_KEY).asText(StringUtils.EMPTY);
        if (StringUtils.isEmpty(sessionId)) {
            sessionId = CommandConst.SESSION_COMMON;
        }
        switch (action) {
            case CommandConst.ACTION_NOTICE_INFO:
            case CommandConst.ACTION_NOTICE_WARN:
            case CommandConst.ACTION_NOTICE_ERROR:
                NoticeEnum level = EnumUtils.getEnum(NoticeEnum.class, action);
                WebSocketManager.getInstance().notice(param, level);
                break;
            case CommandConst.ACTION_RESTART:
                trigRestartEvent(sid);
                break;
            default:
                break;
        }
    }

    private void trigRestartEvent(String sid) {
        TaskEvent taskEvent = new TaskEvent(TaskEventEnum.RESTART);
        ArrayList<String> paths = new ArrayList<>();
        ServerSetting setting = PropertyFileUtils.getServerSettingBySid(sid);
        paths.add(setting.getPath());
        taskEvent.setPaths(paths);
        ApplicationContextUtils.publish(taskEvent);
    }
}
