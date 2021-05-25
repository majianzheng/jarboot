package com.mz.jarboot.base;

import com.google.common.base.Stopwatch;
import com.mz.jarboot.common.Command;
import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.event.AgentOfflineEvent;
import com.mz.jarboot.event.ApplicationContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AgentManager {
    private static volatile AgentManager instance = null;
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
        logger.info("目标进程已退出，唤醒killServer方法的执行线程");
        synchronized (client) {// NOSONAR
            if (ClientState.EXITING.equals(client.getState())) {
                //发送了退出执行，唤醒killClient线程
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
            sendCommand(server, CommandConst.EXIT_CMD, null);
            //等目标进程发送offline信息时执行notify唤醒当前线程
            try {
                client.wait(CommonConst.MAX_WAIT_EXIT_TIME);
            } catch (InterruptedException e) {
                //ignore
                Thread.currentThread().interrupt();
            }
            logger.info("等待目标进程退出完成,耗时:{} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            if (clientMap.containsKey(server)) {
                logger.warn("未能成功退出！{}", server);
                //失败
                return false;
            } else {
                client.setState(ClientState.OFFLINE);
            }
        }
        return true;
    }

    public void sendCommand(String server, String cmd, String param) {
        AgentClient client = clientMap.getOrDefault(server, null);
        if (null != client) {
            client.sendCommand(cmd, param);
        }
    }

    public CommandResponse sendCommandSync(String server, Command command) {
        AgentClient client = clientMap.getOrDefault(server, null);
        if (null == client) {
            CommandResponse resp = new CommandResponse();
            resp.setResultCode(ResultCodeConst.VALIDATE_FAILED);
            resp.setResultMsg("目标进程已经处于失联状态");
            return resp;
        }
        return client.sendCommand(command);
    }

    public void onAck(String server, CommandResponse resp) {
        AgentClient client = clientMap.getOrDefault(server, null);
        if (null != client) {
            client.onAck(resp);
        }
    }
}
