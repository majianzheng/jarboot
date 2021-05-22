package com.mz.jarboot.core.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mz.jarboot.core.advisor.TransformerManager;
import com.mz.jarboot.core.msg.HandleMsgRecv;
import com.mz.jarboot.core.msg.MsgRecv;
import com.mz.jarboot.core.ws.MessageHandler;
import com.mz.jarboot.core.ws.WebSocketClient;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.util.Base64;

public class JarbootBootstrap {
    private static JarbootBootstrap bootstrap;
    private TransformerManager transformerManager;
    private WebSocketClient client;
    private HandleMsgRecv handler;
    private String host;
    private String serverName;
    private boolean online = false;
    public static PrintStream ps = System.err; // NOSONAR
    static {
        try {
            File logDir = new File(System.getProperty("user.home") + File.separator +
                    "jarboot" + File.separator + "logs"  + File.separator);
            if (!logDir.exists()) {
                logDir.mkdir(); // NOSONAR
            }
            File log = new File(logDir, "jarboot-agent.log");
            if (!log.exists()) {
                log.createNewFile(); // NOSONAR
            }
            ps = new PrintStream(new FileOutputStream(log, true));
        } catch (Throwable e) { // NOSONAR
            e.printStackTrace(ps);
        }
    }
    private JarbootBootstrap(Instrumentation inst, String args) {
        ps.println("构造函数>>>");
        transformerManager = new TransformerManager(inst);
        if (null == args || args.isEmpty()) {
            return;
        }
        //解析args，获取目标服务端口
        String s = new String(Base64.getDecoder().decode(args));
        ps.println("解码>>>" + s);
        JSONObject json = JSON.parseObject(s);
        host = json.getString("host");
        serverName = json.getString("server");
        ps.println("获取参数>>>" + host + ", server: " + serverName);
        this.initClient();
    }
    private void initClient() {
        String url = String.format("ws://%s/jarboot-agent/ws", host);
        ps.println("initClient>>>" + url);
        client = new WebSocketClient(url);
        handler = new HandleMsgRecv(host);
        boolean isOk = client.connect(new MessageHandler() {
            @Override
            public void onOpen(Channel channel) {
                online = true;
                ps.println("连接成功>>>");
                //通知主控服务上线
                channel.writeAndFlush(new TextWebSocketFrame(formatSendMsg("online", serverName)));
            }

            @Override
            public void onText(String text, Channel channel) {
                ps.println("收到消息>>>" + text);
                MsgRecv recv = JSON.parseObject(text, MsgRecv.class);
                handler.onMsgRecv(recv);
            }

            @Override
            public void onBinary(byte[] bytes, Channel channel) {
                ps.println("收到消息>>>");
                MsgRecv recv = JSON.parseObject(bytes, MsgRecv.class);
                handler.onMsgRecv(recv);
            }

            @Override
            public void onClose(Channel channel) {
                online = false;
                ps.println("连接关闭>>>");
                client.sendText(formatSendMsg("offline", serverName));
            }

            @Override
            public void onError(Channel channel) {
                ps.println("连接异常>>>");
                onClose(channel);
            }
        });
        ps.println("连接结果>>>" + isOk);
        client.sendText(formatSendMsg("online", serverName));
    }

    private String formatSendMsg(String event, String body) {
        JSONObject object = new JSONObject();
        object.put("event", event);
        object.put("body", body);
        return object.toJSONString();
    }

    public boolean isOnline() {
        return online;
    }

    public synchronized static JarbootBootstrap getInstance(Instrumentation inst, String args) {
        ps.println("getInstance>>>" + args);
        if (bootstrap != null) {
            return bootstrap;
        }
        bootstrap = new JarbootBootstrap(inst, args);
        return bootstrap;
    }
    public static JarbootBootstrap getInstance() {
        if (null == bootstrap) {
            throw new IllegalStateException("Jarboot must be initialized before!");
        }
        return bootstrap;
    }
}
