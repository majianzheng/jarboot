package com.mz.jarboot.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.mz.jarboot.api.pojo.JvmProcess;
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
import org.apache.commons.lang3.math.NumberUtils;
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
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /** 客户端Map */
    private final ConcurrentHashMap<String, AgentClient> clientMap = new ConcurrentHashMap<>(16);
    /** 启动中的latch */
    private final ConcurrentHashMap<String, CountDownLatch> startingLatchMap = new ConcurrentHashMap<>(16);
    /** 本地进程的pid列表 */
    private final ConcurrentHashMap<Integer, String> localProcesses = new ConcurrentHashMap<>(16);
    /** 远程进程列表 */
    private final ConcurrentHashMap<String, JvmProcess> remoteProcesses = new ConcurrentHashMap<>(16);
    /** 优雅退出最大等待时间 */
    private int maxGracefulExitTime = CommonConst.MAX_WAIT_EXIT_TIME;

    /**
     * 单例获取
     * @return 单例
     */
    public static AgentManager getInstance() {
        return AgentManagerHolder.INSTANCE;
    }

    /**
     * 进程上线
     * @param server 服务名
     * @param session 会话
     * @param sid sid服务唯一id
     */
    public void online(String server, Session session, String sid) {
        //目标进程上线
        AgentClient client = new AgentClient(server, sid, session);
        clientMap.put(sid, client);
        CountDownLatch latch = startingLatchMap.getOrDefault(sid, null);
        if (null == latch) {
            client.setState(ClientState.ONLINE);
        } else {
            latch.countDown();
        }
        int pid = TaskUtils.getPid(sid);
        if (pid > 0) {
            //属于受管理的服务
            localProcesses.put(pid, sid);
            client.setPid(pid);
        } else {
            //非受管理的本地进程，通知前端Attach成功
            if (StringUtils.startsWith(sid, CommonConst.REMOTE_SID_PREFIX)) {
                this.remoteJvm(sid);
            } else {
                WebSocketManager.getInstance().debugProcessEvent(sid, AttachStatus.ATTACHED);
            }
        }
    }

    /**
     * 进程下线
     * @param server 服务名
     * @param sid sid服务唯一id
     */
    public void offline(String server, String sid) {
        final AgentClient client = clientMap.getOrDefault(sid, null);
        if (null == client) {
            return;
        }
        int pid = client.getPid();
        if (pid > 0) {
            localProcesses.remove(pid);
        } else {
            if (StringUtils.startsWith(sid, CommonConst.REMOTE_SID_PREFIX)) {
                remoteProcesses.remove(sid);
            }
            //非受管理的本地进程，通知前端进程已经离线
            WebSocketManager.getInstance().debugProcessEvent(sid, AttachStatus.EXITED);
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

    /**
     * 检查服务是否在线
     * @param sid 服务id
     * @return 是否在线
     */
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

    /**
     * 来自远程服务器的连接
     * @param sid 远程服务sid，格式：prefix + pid + name + uuid
     */
    public void remoteJvm(final String sid) {
        JvmProcess process = new JvmProcess();
        int index = sid.lastIndexOf(',');
        if (-1 == index) {
            logger.warn("解析远程sid失败！sid:{}", sid);
            return;
        }
        String str = sid.substring(0, index);
        final int limit = 4;
        String[] s = str.split(CommonConst.COMMA_SPLIT, limit);
        if (s.length != limit) {
            logger.warn("解析远程sid失败！sid:{}", sid);
            return;
        }
        String pid = s[1];
        String remoteIp = s[2];
        String name = s[3];
        process.setAttached(true);
        process.setPid(NumberUtils.toInt(pid, CommonConst.INVALID_PID));
        process.setName(name);
        process.setSid(sid);
        process.setRemote(remoteIp);
        remoteProcesses.put(sid, process);
        WebSocketManager.getInstance().debugProcessEvent(sid, AttachStatus.ATTACHED);
    }

    /**
     * 获取远程服务列表
     * @param list 远程服务列表
     */
    public void remoteProcess(ArrayList<JvmProcess> list) {
        if (!remoteProcesses.isEmpty()) {
            list.addAll(remoteProcesses.values());
        }
    }

    /**
     * 杀死客户端
     * @param server 服务名
     * @param sid 服务唯一id
     * @return 是否成功
     */
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

    /**
     * 发送命令
     * @param server 服务名
     * @param sid 唯一id
     * @param command 命令
     * @param sessionId 会话id
     */
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
                    WebSocketManager
                            .getInstance()
                            .commandEnd(sid, server + "连接断开，重连失败，请稍后重试", sessionId);
                } else {
                    client.sendCommand(command, sessionId);
                }
            } else {
                //未在线，进程不存在
                WebSocketManager
                        .getInstance()
                        .commandEnd(sid, server + "未在线，无法执行命令", sessionId);
            }
        } else {
            client.sendCommand(command, sessionId);
        }
    }

    /**
     * 尝试重新连接
     * @param server 服务名
     * @param sid 服务唯一id
     * @param sessionId 会话id
     */
    private void tryReConnect(String server, String sid, String sessionId) {
        CountDownLatch latch = startingLatchMap.computeIfAbsent(sid, k -> new CountDownLatch(1));
        try {
            TaskUtils.attach(sid);
            WebSocketManager.getInstance().sendConsole(sid, server + "连接断开，重连中...", sessionId);
            if (!latch.await(CommonConst.MAX_AGENT_CONNECT_TIME, TimeUnit.SECONDS)) {
                logger.error("Attach and wait server connect timeout，{}", server);
                WebSocketManager.getInstance().sendConsole(sid, server + "Attach重连超时！", sessionId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            startingLatchMap.remove(sid);
        }
    }

    /**
     * 发送内部指令
     * @param sid 服务唯一id
     * @param command 命令
     * @param sessionId 会话id
     */
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

    /**
     * Agent客户端的响应内容处理
     * @param server 服务名
     * @param sid 唯一id
     * @param resp 响应消息体
     * @param session 会话
     */
    public void handleAgentResponse(String server, String sid, CommandResponse resp, Session session) {
        ResponseType type = resp.getResponseType();
        String sessionId = resp.getSessionId();
        switch (type) {
            case HEARTBEAT:
                doHeartbeat(server, sid, session);
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
                commandEnd(sid, resp, sessionId);
                break;
            case ACTION:
                this.handleAction(resp.getBody(), sessionId, sid);
                break;
            default:
                logger.error("Unknown response type.type:{}, sid:{},server:{}", type, sid, server);
                break;
        }
    }

    private void commandEnd(String sid, CommandResponse resp, String sessionId) {
        String msg = resp.getBody();
        if (StringUtils.isNotEmpty(msg) && Boolean.FALSE.equals(resp.getSuccess())) {
            msg = String.format("<span style=\"color:red\">%s</span>", resp.getBody());
        }
        WebSocketManager.getInstance().commandEnd(sid, msg, sessionId);
    }

    private void doHeartbeat(String server, String sid, Session session) {
        AgentClient client = clientMap.getOrDefault(sid, null);
        if (null == client || !ClientState.ONLINE.equals(client.getState())) {
            this.online(server, session, sid);
            this.onServerStarted(server, sid);
            WebSocketManager.getInstance().sendConsole(sid, server + " reconnected by heartbeat!");
            logger.info("reconnected by heartbeat {}, {}", server, sid);
            WebSocketManager.getInstance().publishStatus(sid, TaskStatus.RUNNING);
        }
        client.heartbeat();
    }

    /**
     * 进程启动完成事件响应
     * @param server 服务名
     * @param sid 服务唯一id
     */
    public void onServerStarted(final String server, final String sid) {
        AgentClient client = clientMap.getOrDefault(sid, null);
        if (null == client) {
            logger.error("Server {} is offline already!", server);
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

    /**
     * 等待服务启动完成
     * @param server 服务名
     * @param sid 服务唯一id
     * @param millis 时间
     */
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
                WebSocketManager
                        .getInstance()
                        .sendConsole(sid,
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

    /**
     * 浏览器刷新或关闭，重置进程会话
     * @param sessionId 会话id
     */
    public void releaseAgentSession(String sessionId) {
        //向所有在线的agent客户端发送会话失效命令
        clientMap.forEach((k, v) -> sendInternalCommand(v.getSid(), CommandConst.CANCEL_CMD, sessionId));
    }

    /**
     * 设置优雅退出最大等待时间
     * @param d 时间
     */
    public void setMaxGracefulExitTime(int d) {
        this.maxGracefulExitTime = d;
    }

    /**
     * 获取优雅退出最大等待时间
     * @return 时间
     */
    public int getMaxGracefulExitTime() {
        return this.maxGracefulExitTime;
    }

    /**
     * 判断进程pid是否为受Jarboot管理的进程，即由Jarboot所启动的进程
     * @param pid 进程pid
     * @return 是否为受Jarboot管理的进程
     */
    public boolean isManageredServer(int pid) {
        return localProcesses.containsKey(pid);
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
                WebSocketManager.getInstance().notice(param, level, sessionId);
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

    private static class AgentManagerHolder {
        static final AgentManager INSTANCE = new AgentManager();
    }

    private AgentManager(){}
}
