package com.mz.jarboot.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.JsonUtils;
import com.mz.jarboot.common.ResponseType;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.event.*;
import com.mz.jarboot.task.TaskStatus;
import com.mz.jarboot.utils.TaskUtils;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.websocket.Session;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author majianzheng
 */
@SuppressWarnings("all")
public class AgentManager {
    private static volatile AgentManager instance = null;
    private final ConcurrentHashMap<String, AgentClient> clientMap = new ConcurrentHashMap<>(16);
    private final ConcurrentHashMap<String, CountDownLatch> startingSemMap = new ConcurrentHashMap<>(16);
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private int maxGracefulExitTime = CommonConst.MAX_WAIT_EXIT_TIME;
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
        CountDownLatch latch = startingSemMap.getOrDefault(server, null);
        if (null != latch) {
            latch.countDown();
        }
    }

    public void offline(String server) {
        final AgentClient client = clientMap.getOrDefault(server, null);
        if (null == client) {
            return;
        }
        WebSocketManager.getInstance().sendConsole(server, server + "下线！");
        synchronized (client) {
            //同时判定STARTING，因为启动可能会失败，需要唤醒等待启动完成的线程
            if (ClientState.EXITING.equals(client.getState()) || ClientState.STARTING.equals(client.getState())) {
                //发送了退出执行，唤醒killClient或waitServerStarted线程
                client.notify();
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
        synchronized (client) {
            return ClientState.ONLINE.equals(client.getState());
        }
    }

    public boolean killClient(String server) {
        final AgentClient client = clientMap.getOrDefault(server, null);
        if (null == client) {
            logger.debug("服务已经是退出状态，{}", server);
            return false;
        }
        synchronized (client) {
            long startTime = System.currentTimeMillis();
            client.setState(ClientState.EXITING);
            sendInternalCommand(server, CommandConst.EXIT_CMD, CommandConst.SESSION_COMMON);
            //等目标进程发送offline信息时执行notify唤醒当前线程
            try {
                client.wait(maxGracefulExitTime);
            } catch (InterruptedException e) {
                //ignore
                Thread.currentThread().interrupt();
            }
            long costTime = System.currentTimeMillis() - startTime;
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
            //如果进程仍然存活
            int pid = TaskUtils.getServerPid(server);
            if (-1 != pid) {
                TaskUtils.attach(server, pid);
                WebSocketManager.getInstance().commandEnd(server, "连接断开，重连中...", sessionId);
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                client = clientMap.getOrDefault(server, null);
                if (null != client) {
                    client.sendCommand(command, sessionId);
                } else {
                    WebSocketManager.getInstance().commandEnd(server, "连接断开，重连超时，请稍后重试", sessionId);
                }
            } else {
                WebSocketManager.getInstance().commandEnd(server, "未在线，无法执行命令", sessionId);
            }
        } else {
            client.sendCommand(command, sessionId);
        }
    }

    public void sendInternalCommand(String server, String command, String sessionId) {
        if (StringUtils.isEmpty(server) || StringUtils.isEmpty(command)) {
            WebSocketManager.getInstance().commandEnd(server, StringUtils.EMPTY, sessionId);
            return;
        }
        AgentClient client = clientMap.getOrDefault(server, null);
        if (null == client) {
            WebSocketManager.getInstance().commandEnd(server, StringUtils.EMPTY, sessionId);
            return;
        }
        client.sendInternalCommand(command, sessionId);
    }

    public void handleAgentResponse(String server, CommandResponse resp) {
        ResponseType type = resp.getResponseType();
        String sessionId = resp.getSessionId();
        switch (type) {
            case CONSOLE:
                WebSocketManager.getInstance().sendConsole(server, resp.getBody(), sessionId);
                break;
            case STD_OUT:
                //启动中的控制台消息
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
            case ACTION:
                this.handleAction(resp.getBody(), sessionId, server);
                break;
            default:
                //do nothing
                break;
        }
    }

    public void onServerStarted(final String server) {
        AgentClient client = clientMap.getOrDefault(server, null);
        if (null == client) {
            return;
        }
        client = clientMap.getOrDefault(server, null);
        if (null == client) {
            logger.error("Server {} in offline already!", server);
            WebSocketManager.getInstance().sendConsole(server, server + " is offline now！");
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

    public void waitServerStarted(String server, int millis) {
        AgentClient client = clientMap.getOrDefault(server, null);
        if (null == client) {
            CountDownLatch latch = startingSemMap.computeIfAbsent(server, k -> new CountDownLatch(1));
            try {
                if (!latch.await(CommonConst.MAX_AGENT_CONNECT_TIME, TimeUnit.SECONDS)) {
                    logger.error("Wait server connect timeout，{}", server);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                startingSemMap.remove(server);
            }
            client = clientMap.getOrDefault(server, null);
            if (null == client) {
                WebSocketManager.getInstance().sendConsole(server, server + " connect timeout！");
                return;
            }
        }

        synchronized (client) {
            if (!ClientState.STARTING.equals(client.getState())) {
                logger.info("Current server({}) is not starting now, wait server started error. statue:{}",
                        server, client.getState());
                WebSocketManager.getInstance().sendConsole(server,
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
        clientMap.forEach((k, v) -> sendInternalCommand(k, CommandConst.CANCEL_CMD, sessionId));
    }

    public void setMaxGracefulExitTime(int d) {
        this.maxGracefulExitTime = d;
    }

    public int getMaxGracefulExitTime() {
        return this.maxGracefulExitTime;
    }

    private void handleAction(String data, String sessionId, String server) {
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
                TaskEvent taskEvent = new TaskEvent();
                taskEvent.setEventType(TaskEventEnum.RESTART);
                ArrayList<String> services = new ArrayList<>();
                services.add(server);
                taskEvent.setServices(services);
                break;
            default:
                break;
        }
    }
}
