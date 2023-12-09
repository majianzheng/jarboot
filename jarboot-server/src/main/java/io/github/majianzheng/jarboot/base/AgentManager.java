package io.github.majianzheng.jarboot.base;

import io.github.majianzheng.jarboot.api.constant.SettingPropConst;
import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.api.pojo.JvmProcess;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;
import io.github.majianzheng.jarboot.common.AnsiLog;
import io.github.majianzheng.jarboot.common.PidFileHelper;
import io.github.majianzheng.jarboot.common.notify.DefaultPublisher;
import io.github.majianzheng.jarboot.common.notify.NotifyReactor;
import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.common.protocol.CommandConst;
import io.github.majianzheng.jarboot.common.protocol.CommandResponse;
import io.github.majianzheng.jarboot.common.protocol.ResponseType;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.task.AttachStatus;
import io.github.majianzheng.jarboot.utils.MessageUtils;
import io.github.majianzheng.jarboot.utils.PropertyFileUtils;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import io.github.majianzheng.jarboot.utils.TaskUtils;
import io.github.majianzheng.jarboot.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.websocket.Session;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author majianzheng
 */
@SuppressWarnings({"squid:S2274", "PrimitiveArrayArgumentToVarargsMethod"})
public class AgentManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /** 客户端Map */
    private final ConcurrentHashMap<String, AgentOperator> clientMap = new ConcurrentHashMap<>(16);
    /** 启动中的latch */
    private final ConcurrentHashMap<String, CountDownLatch> startingLatchMap = new ConcurrentHashMap<>(16);
    /** 本地进程的pid列表 */
    private final ConcurrentHashMap<String, String> localServices = new ConcurrentHashMap<>(16);
    /** 远程进程列表 */
    private final ConcurrentHashMap<String, JvmProcess> remoteProcesses = new ConcurrentHashMap<>(16);

    /** 写日志方法 */
    private java.lang.reflect.Method writeBytes = null;
    /** 当前默认的日志Appender */
    private ch.qos.logback.core.OutputStreamAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = null;
    /** 激活的窗口sid -> sessionId */
    private final Map<String, Set<String>> activeWindow = new ConcurrentHashMap<>(16);

    /**
     * 单例获取
     * @return 单例
     */
    public static AgentManager getInstance() {
        return AgentManagerHolder.INSTANCE;
    }

    /**
     * 进程上线
     * @param userDir 用户目录
     * @param serviceName 服务名
     * @param session 会话
     * @param sid sid服务唯一id
     */
    public void online(String userDir, String serviceName, Session session, String sid) {
        //目标进程上线
        AgentOperator client = clientMap.compute(sid, (k,v) -> new AgentOperator(userDir, serviceName, sid, session));
        syncActiveWindow(sid);
        CountDownLatch latch = startingLatchMap.getOrDefault(sid, null);
        if (null != latch) {
            latch.countDown();
        }
        ServiceSetting setting = PropertyFileUtils.getServiceSetting(userDir, serviceName);
        if (Objects.equals(sid, setting.getSid())) {
            //属于受管理的服务
            String pid = TaskUtils.getPid(sid);
            localServices.put(pid, sid);
            client.setPid(pid);
            client.setTrusted(true);
            client.setSetting(setting);
            ServiceOnlineEvent event = new ServiceOnlineEvent(setting);
            NotifyReactor.getInstance().publishEvent(event);
            boolean needNotifyStatus = (null == latch && ClientState.OFFLINE.equals(client.getState()));
            if (needNotifyStatus && !SettingPropConst.SCHEDULE_CRON.equals(setting.getScheduleType())) {
                MessageUtils.upgradeStatus(sid, CommonConst.RUNNING);
            }
        } else {
            //非受管理的本地进程，通知前端Attach成功
            if (sid.startsWith(CommonConst.REMOTE_SID_PREFIX)) {
                this.remoteJvm(client);
            } else {
                //本地进程默认受信任
                client.setTrusted(true);
                MessageUtils.upgradeStatus(sid, AttachStatus.ATTACHED);
            }
            if (serviceName.endsWith(CommonConst.POST_EXCEPTION_TASK_SUFFIX)) {
                clientMap.remove(sid);
                return;
            }
        }
        if (null == latch) {
            client.setState(ClientState.ONLINE);
        }
    }

    private void syncActiveWindow(String sid) {
        Set<String> activeSession = activeWindow.get(sid);
        if (null == activeSession || activeSession.isEmpty()) {
            sendInternalCommand(sid, "window", StringUtils.EMPTY);
            return;
        }
        String sessions = String.join(",", activeSession);
        windowActive(sid, sessions, true);
    }

    /**
     * 进程下线
     * @param sid sid服务唯一id
     */
    public void offline(String sid) {
        final AgentOperator client = clientMap.remove(sid);
        if (null == client) {
            return;
        }
        String pid = client.getPid();
        if (pid.isEmpty()) {
            if (sid.startsWith(CommonConst.REMOTE_SID_PREFIX)) {
                remoteProcesses.remove(sid);
            }
            //非受管理的本地进程，通知前端进程已经离线
            MessageUtils.upgradeStatus(sid, AttachStatus.EXITED);
        } else {
            localServices.remove(pid);
        }
        String msg = String.format("\033[1;96m%s\033[0m 下线！", client.getName());
        MessageUtils.console(sid, msg);
        synchronized (client) {
            boolean stopping = ClientState.EXITING.equals(client.getState());
            //发送了退出执行，唤醒killClient或waitServerStarted线程
            try {
                client.notifyAll();
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            } finally {
                //先移除，防止再次点击终止时，会去执行已经关闭的会话
                //此时属于异常退出，发布异常退出事件，通知任务守护服务
                ServiceSetting setting = client.getSetting();
                if (null != setting) {
                    ServiceOfflineEvent event = new ServiceOfflineEvent(setting, stopping);
                    NotifyReactor.getInstance().publishEvent(event);
                }
                client.setState(ClientState.OFFLINE);
                if (null == setting || !SettingPropConst.SCHEDULE_CRON.equals(setting.getScheduleType())) {
                    MessageUtils.upgradeStatus(sid, CommonConst.STOPPED);
                }
            }
        }
    }

    /**
     * 检查服务是否在线
     * @param sid 服务id
     * @return 是否在线
     */
    public boolean isOnline(String sid) {
        final AgentOperator client = clientMap.getOrDefault(sid, null);
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
     * 坚持是否存在
     * @param sid sid
     * @return 是否存在
     */
    public boolean exist(String sid) {
        return clientMap.containsKey(sid);
    }

    /**
     * 获取远程服务列表
     * @return 远程服务列表
     */
    public List<JvmProcess> remoteProcess() {
        return new ArrayList<>(this.remoteProcesses.values());
    }

    /**
     * 优雅退出客户端
     * @param sid 服务唯一id
     * @return 是否成功
     */
    public boolean gracefulExit(String sid) {
        final AgentOperator client = clientMap.getOrDefault(sid, null);
        if (null == client) {
            return false;
        }
        String pid = TaskUtils.getPid(sid);
        if (StringUtils.isEmpty(pid)) {
            logger.info("进程({})已经退出", sid);
            return true;
        }
        int maxExitTime = SettingUtils.getSystemSetting().getMaxExitTime();
        synchronized (client) {
            long startTime = System.currentTimeMillis();
            client.setState(ClientState.EXITING);
            sendInternalCommand(sid, CommandConst.EXIT_CMD, StringUtils.EMPTY);
            //等目标进程发送offline信息时执行notify唤醒当前线程
            try {
                // 连结断开
                client.wait(maxExitTime);
            } catch (InterruptedException e) {
                //ignore
                Thread.currentThread().interrupt();
            }
            long costTime = System.currentTimeMillis() - startTime;
            if (clientMap.containsKey(sid)) {
                logger.warn("未能成功退出！{}, 耗时:{}，将执行强制杀死命令", sid, costTime);
                MessageUtils.warn("服务(sid:" + sid + ")未等到退出消息，将执行强制退出命令！");
                return false;
            } else {
                try {
                    // 等待进程真正退出
                    TimeUnit.MILLISECONDS.sleep(500);
                    while (TaskUtils.checkProcessAlive(pid) && (System.currentTimeMillis() - startTime) <= maxExitTime) {
                        AnsiLog.info("等待进程退出，pid:{}, sid: {}", pid, sid);
                        TimeUnit.SECONDS.sleep(1);
                    }
                } catch (InterruptedException e) {
                    //ignore
                    Thread.currentThread().interrupt();
                }
                if (TaskUtils.checkProcessAlive(pid)) {
                    // 等待超时
                    MessageUtils.console(sid, "进程优雅退出失败，将强制杀死进程！");
                    TaskUtils.killByPid(pid);
                    PidFileHelper.deletePidFile(sid);
                    clientMap.remove(sid);
                } else {
                    MessageUtils.console(sid, "进程优雅退出成功！");
                }
                client.setState(ClientState.OFFLINE);
            }
        }
        return true;
    }

    /**
     * 判断进程pid是否为受Jarboot管理的进程，即由Jarboot所启动的进程
     * @param pid 进程pid
     * @return 是否为受Jarboot管理的进程
     */
    public boolean isLocalService(String pid) {
        return localServices.containsKey(pid);
    }

    /**
     * 等待服务启动完成
     * @param setting 服务配置
     * @param millis 时间
     * @param callback 任务
     */
    public void waitServiceStarted(ServiceSetting setting, int millis, Runnable callback) {
        String sid = setting.getSid();
        AgentOperator client = clientMap.getOrDefault(sid, null);
        if (null == client) {
            CountDownLatch latch = startingLatchMap.computeIfAbsent(sid, k -> new CountDownLatch(1));
            try {
                callback.run();
                if (!latch.await(CommonConst.MAX_AGENT_CONNECT_TIME, TimeUnit.SECONDS)) {
                    logger.error("Wait service connect timeout, sid:{}", sid);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                MessageUtils.error("Start task error " + e.getMessage());
            } finally {
                startingLatchMap.remove(sid);
            }
            client = clientMap.getOrDefault(sid, null);
            if (null == client) {
                String msg = formatErrorMsg(sid, "connect timeout!");
                MessageUtils.console(sid, msg);
                return;
            }
        } else {
            // 已经启动
            return;
        }

        synchronized (client) {
            if (!ClientState.STARTING.equals(client.getState())) {
                logger.info("Current service({}) is not starting now, wait service started error. statue:{}",
                        client.getName(), client.getState());
                MessageUtils.console(sid,
                        client.getName() + " is not starting, wait started error. status:" + client.getState());
                return;
            }
            try {
                client.wait(millis);
            } catch (InterruptedException e) {
                //ignore
                Thread.currentThread().interrupt();
            }
            logger.debug("{}等待启动完成！", client.getName());
        }
    }

    /**
     * 添加信任的服务
     * @param host 地址
     */
    public void addTrustedHost(String host) {
        remoteProcesses.forEach((k, v) -> {
            if (Objects.equals(v.getRemote(), host)) {
                AgentOperator client = clientMap.getOrDefault(k, null);
                if (null != client) {
                    client.setTrusted(true);
                    v.setTrusted(true);
                    MessageUtils.upgradeStatus(k, AttachStatus.TRUSTED);
                }
            }
        });
    }

    /**
     * 来自远程服务器的连接
     * @param client 远程服务sid，格式：prefix + pid + name + uuid
     */
    private void remoteJvm(final AgentOperator client) {
        String sid = client.getSid();
        JvmProcess process = new JvmProcess();
        int index = sid.lastIndexOf(',');
        if (-1 == index) {
            return;
        }
        String str = sid.substring(0, index);
        final int limit = 4;
        String[] s = str.split(CommonConst.COMMA_SPLIT, limit);
        if (s.length != limit) {
            return;
        }
        String pid = s[1];
        String remoteIp = s[2];
        String name = s[3];
        process.setStatus(CommonConst.ATTACHED);
        process.setPid(pid);
        process.setName(name);
        process.setSid(sid);
        process.setRemote(remoteIp);
        boolean isTrusted = SettingUtils.isTrustedHost(remoteIp);
        client.setTrusted(isTrusted);
        remoteProcesses.put(sid, process);
        MessageUtils.upgradeStatus(sid, AttachStatus.ATTACHED);
    }

    /**
     * 发送命令
     * @param sid 唯一id
     * @param command 命令
     * @param sessionId 会话id
     * @param col 宽
     * @param row 高
     */
    private void sendCommand(String sid, String command, String sessionId, int col, int row) {
        if (StringUtils.isEmpty(sid) || StringUtils.isEmpty(command)) {
            return;
        }
        AgentOperator client = clientMap.getOrDefault(sid, null);
        if (null == client) {
            if (TaskUtils.getPid(sid).isEmpty()) {
                String msg = formatErrorMsg(StringUtils.EMPTY, "未在线，无法执行命令");
                //未在线，进程不存在
                MessageUtils.commandFailed(sid, sessionId, msg);
            } else {
                //如果进程仍然存活，尝试使用attach重新连接
                tryReConnect(sid, sessionId);

                client = clientMap.getOrDefault(sid, null);
                if (null == client) {
                    String msg = formatErrorMsg(sid, "连接断开，重连失败，请稍后重试");
                    MessageUtils.commandFailed(sid, sessionId, msg);
                }
            }
        }
        if (null != client) {
            if (client.isTrusted()) {
                client.sendCommand(command, sessionId, row, col);
            } else {
                String msg = formatErrorMsg(StringUtils.EMPTY, "not trusted!");
                MessageUtils.commandFailed(sid, sessionId, msg);
                MessageUtils.upgradeStatus(sid, AttachStatus.NOT_TRUSTED);
            }
        }
    }

    /**
     * 尝试重新连接
     * @param sid 服务唯一id
     * @param sessionId 会话id
     */
    private void tryReConnect(String sid, String sessionId) {
        CountDownLatch latch = startingLatchMap.computeIfAbsent(sid, k -> new CountDownLatch(1));
        try {
            TaskUtils.attach(sid);
            MessageUtils.console(sid, sessionId, "连接断开，重连中...");
            if (!latch.await(CommonConst.MAX_AGENT_CONNECT_TIME, TimeUnit.SECONDS)) {
                logger.error("Attach and wait service connect timeout，{}", sid);
                MessageUtils.console(sid, sessionId, "Attach重连超时！");
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
    private void sendInternalCommand(String sid, String command, String sessionId) {
        if (StringUtils.isEmpty(sid) || StringUtils.isEmpty(command)) {
            MessageUtils.commandFailed(sid, sessionId, StringUtils.EMPTY);
            return;
        }
        AgentOperator client = clientMap.getOrDefault(sid, null);
        if (null == client) {
            MessageUtils.commandFailed(sid, sessionId, StringUtils.EMPTY);
            return;
        }
        client.sendInternalCommand(command, sessionId);
    }

    /**
     * Agent客户端的响应内容处理
     * @param event
     */
    private void onResponse(AgentResponseEvent event) {
        CommandResponse resp = event.getResponse();
        ResponseType type = resp.getResponseType();
        switch (type) {
            case HEARTBEAT:
                doHeartbeat(event.getUserDir(), event.getServiceName(), event.getSid(), event.getSession());
                break;
            case STD_PRINT:
                //启动中的控制台消息
                MessageUtils.stdPrint(event.getResponse().getSessionId(), event.getSid(), resp.getBody());
                break;
            case LOG_APPENDER:
                onAgentLog(event.getSid(), resp.getBody());
                break;
            case NOTIFY:
                this.onNotify(resp, event.getSid());
                break;
            default:
                //ignore
                break;
        }
    }

    private void trustOnce(String sid) {
        AgentOperator client = clientMap.getOrDefault(sid, null);
        if (null != client && !client.isTrusted()) {
            client.setTrusted(true);
            MessageUtils.upgradeStatus(sid, AttachStatus.TRUSTED);
        }
    }

    private boolean checkNotTrusted(String sid) {
        AgentOperator client = clientMap.getOrDefault(sid, null);
        if (null == client) {
            return true;
        }
        if (client.isTrusted()) {
            return false;
        }
        JvmProcess jvm = remoteProcesses.getOrDefault(sid, null);
        if (null == jvm) {
            return true;
        }
        if (Boolean.TRUE.equals(jvm.getTrusted())) {
            client.setTrusted(true);
            return false;
        }
        if (SettingUtils.isTrustedHost(jvm.getRemote())) {
            jvm.setTrusted(true);
            client.setTrusted(true);
            return false;
        }
        return true;
    }

    private void onAgentLog(String sid, String msg) {
        if (checkNotTrusted(sid)) {
            return;
        }
        if (null != writeBytes) {
            try {
                writeBytes.invoke(appender, msg.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                //ignore
            }
        }
    }

    private void doHeartbeat(String userDir, String serviceName, String sid, Session session) {
        if (null == session) {
            return;
        }
        Set<String> sessionIds = activeWindow.get(sid);
        AgentOperator client = clientMap.getOrDefault(sid, null);
        if (null == client) {
            String path = SettingUtils.getServicePath(userDir, serviceName);
            if (!Objects.equals(SettingUtils.createSid(path), sid) && serviceName.endsWith(CommonConst.POST_EXCEPTION_TASK_SUFFIX)) {
                new AgentOperator(userDir, serviceName, sid, session).heartbeat(sessionIds);
                AnsiLog.debug("异常离线脚本执行，心跳探测");
                return;
            }
            this.online(userDir, serviceName, session, sid);
            this.onServiceStarted(sid);
            MessageUtils.info(serviceName + "恢复连接!");
            AnsiLog.debug("reconnected by heartbeat {}, {}", serviceName, sid);
            client = clientMap.getOrDefault(sid, null);
            if (null != client) {
                client.heartbeat(sessionIds);
            }
        } else {
            client.heartbeat(sessionIds);
        }
    }

    /**
     * 进程启动完成事件响应
     * @param sid 服务唯一id
     */
    private void onServiceStarted(final String sid) {
        AgentOperator client = clientMap.getOrDefault(sid, null);
        if (null == client) {
            return;
        }
        synchronized (client) {
            //发送启动成功，唤醒waitServerStarted线程
            try {
                client.notifyAll();
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            } finally {
                client.setState(ClientState.ONLINE);
            }
        }
    }

    /**
     * 浏览器刷新或关闭，重置进程会话
     * @param sessionId 会话id
     */
    private void releaseAgentSession(String sessionId) {
        //向所有在线的agent客户端发送会话失效命令
        clientMap.forEach((k, v) -> sendInternalCommand(v.getSid(), CommandConst.INVALID_SESSION_CMD, sessionId));
        Set<String> waitDelete = new HashSet<>(16);
        activeWindow.forEach((k, v) -> {
            v.remove(sessionId);
            if (v.isEmpty()) {
                waitDelete.add(k);
            }
        });
        waitDelete.forEach(activeWindow::remove);
    }

    private void onNotify(CommandResponse resp, String sid) {
        if (checkNotTrusted(sid)) {
            return;
        }
        final String sessionId = resp.getSessionId();
        if (StringUtils.isEmpty(sessionId)) {
            MessageUtils.notify(sid, resp.getSuccess(), resp.getBody());
        } else {
            MessageUtils.notify(sid, sessionId, resp.getSuccess(), resp.getBody());
        }
    }

    private String formatErrorMsg(String serviceName, String msg) {
        return String.format("\033[96m%s\033[0m \033[31m%s\033[0m", serviceName, msg);
    }

    private static class AgentManagerHolder {
        static final AgentManager INSTANCE = new AgentManager();
    }

    private void onFuncReceivedEvent(FuncReceivedEvent event) {
        final String sid = event.getSid();
        switch (event.funcCode()) {
            case CMD_FUNC:
                sendCommand(sid, event.getBody(), event.getSessionId(), event.getCols(), event.getRows());
                break;
            case CANCEL_FUNC:
                sendInternalCommand(sid, CommandConst.CANCEL_CMD, event.getSessionId());
                break;
            case TRUST_ONCE_FUNC:
                trustOnce(sid);
                break;
            case TRUST_ALWAYS_FUNC:
                SettingUtils.addTrustedHost(event.getBody());
                addTrustedHost(event.getBody());
                trustOnce(sid);
                break;
            case CHECK_TRUSTED_FUNC:
                if (checkNotTrusted(sid)) {
                    MessageUtils.upgradeStatus(sid, AttachStatus.NOT_TRUSTED);
                }
                break;
            case DETACH_FUNC:
                sendInternalCommand(sid, CommandConst.SHUTDOWN, event.getSessionId());
                break;
            case SESSION_CLOSED_FUNC:
                releaseAgentSession(event.getSessionId());
                break;
            case ACTIVE_WINDOW:
                changeWindowState(event.getSid(), event.getSessionId(), true);
                break;
            case CLOSE_WINDOW:
                changeWindowState(event.getSid(), event.getSessionId(), false);
                break;
            default:
                logger.debug("Unknown func, func:{}", event.funcCode());
                break;
        }
    }

    private void changeWindowState(String sid, String sessionId, boolean active) {
        activeWindow.compute(sid, (k, v) -> {
            if (null == v) {
                v = new HashSet<>(16);
            }
            if (active) {
                v.add(sessionId);
            } else {
                v.remove(sessionId);
            }
            if (v.isEmpty()) {
                return null;
            }
            return v;
        });
        windowActive(sid, sessionId, active);
    }

    private void windowActive(String sid, String sessionId, boolean active) {
        if (clientMap.containsKey(sid)) {
            sendInternalCommand(sid, "window -a " + active, sessionId);
        }
    }

    private void initSubscriber() {
        //Agent客户端响应事件
        NotifyReactor.getInstance().registerSubscriber(new Subscriber<AgentResponseEvent>() {
            @Override
            public void onEvent(AgentResponseEvent event) {
                onResponse(event);
            }

            @Override
            public Class<? extends JarbootEvent> subscribeType() {
                return AgentResponseEvent.class;
            }
        }, new DefaultPublisher(32768, "agent.resp.publisher"));

        //服务启动完成事件
        NotifyReactor.getInstance().registerSubscriber(new Subscriber<ServiceStartedEvent>() {
            @Override
            public void onEvent(ServiceStartedEvent event) {
                onServiceStarted(event.getSid());
            }

            @Override
            public Class<? extends JarbootEvent> subscribeType() {
                return ServiceStartedEvent.class;
            }
        });

        //前端调用事件
        NotifyReactor.getInstance().registerSubscriber(new Subscriber<FuncReceivedEvent>() {
            @Override
            public void onEvent(FuncReceivedEvent event) {
                onFuncReceivedEvent(event);
            }

            @Override
            public Class<? extends JarbootEvent> subscribeType() {
                return FuncReceivedEvent.class;
            }
        }, new DefaultPublisher(16384, "func.req.publisher"));
    }

    @SuppressWarnings("java:S3011")
    private AgentManager() {
        //获取日志的appender，当宿主服务发来日志时，使用当前服务的日志系统记录
        //这里解读了logback日志源码，反射获取写日志文件的类方法
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("ROOT");
        appender = (ch.qos.logback.core.OutputStreamAppender<ch.qos.logback.classic.spi.ILoggingEvent>) root
                .getAppender("FILE");
        try {
            writeBytes = ch.qos.logback.core.OutputStreamAppender.class
                    .getDeclaredMethod("writeBytes", byte[].class);
            writeBytes.setAccessible(true);
        } catch (Exception e) {
            //ignore
        }

        //注册事件订阅
        this.initSubscriber();
    }
}
