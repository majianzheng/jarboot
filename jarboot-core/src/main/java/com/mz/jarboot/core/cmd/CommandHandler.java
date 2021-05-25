package com.mz.jarboot.core.cmd;

import com.alibaba.fastjson.JSON;
import com.mz.jarboot.common.*;
import com.mz.jarboot.core.constant.JarbootCoreConstant;
import com.mz.jarboot.core.ws.WebSocketClient;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 命令处理
 * @author majianzheng
 */
public class CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(JarbootCoreConstant.LOG_NAME);
    private final String url;
    private final WebSocketClient client;
    public CommandHandler(String url, WebSocketClient client) {
        this.url = url;
        this.client = client;
    }
    public void execute(String msg) {
        logger.debug("收到消息：{}", msg);
        Command command;
        try {
            command = JSON.parseObject(msg, Command.class);
        } catch (Exception e) {
            logger.warn("收到的消息格式错误", e);
            return;
        }
        //全部使用小写
        String cmd = command.getCmd().toLowerCase();
        String param = command.getParam();
        logger.debug("开始执行命令：{}", cmd);
        String body = "";
        switch (cmd) {
            case CommandConst.EXIT_CMD:
                this.handleExit();
                break;
            case CommandConst.JVM_CMD:
                body = handleJvmCmd();
                break;
            case CommandConst.SYS_PROP_CMD:
                body = this.handleSysPropCmd(param);
                break;
            case CommandConst.SYS_ENV_CMD:
                body = this.handleSysEnvCmd(param);
                break;
            case CommandConst.THREAD_CMD:
                body = this.handleThread(param);
                break;
            default:
                body = cmd + ": command not found.";
                break;
        }
        if (Boolean.TRUE.equals(command.getAck())) {
            //需要应答
            CommandResponse resp = new ResponseBuilder().setType(CommandConst.ACK_TYPE
            ).setCmd(cmd).setBody(body).getResponse();

            sendTo(resp);
        }
    }

    private void console(String cmd, String body) {
        CommandResponse resp = new ResponseBuilder().setType(CommandConst.CONSOLE_TYPE
        ).setCmd(cmd).setBody(body).getResponse();

        sendTo(resp);
    }

    private void sendTo(CommandResponse resp) {
        String response = JSON.toJSONString(resp);
        if (response.length() < JarbootCoreConstant.MAX_WS_SEND) {
            client.sendText(response);
        } else {
            //发送的数据量较大时通过post，因为服务端为了最大连接数，将接收缓存配置的较小
            httpPost(response);
        }
    }

    private void httpPost(String data) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), data);
        Request.Builder requestBuilder = new Request
                .Builder()
                .url(url)
                .post(requestBody);
        requestBuilder.addHeader("Cookie", "");
        requestBuilder.addHeader("Accept", "application/json");
        requestBuilder.addHeader("Content-Type", "application/json;charset=UTF-8");
        Request request = requestBuilder.build();
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(30L, TimeUnit.SECONDS)
                .readTimeout(30L, TimeUnit.SECONDS)
                .writeTimeout(30L, TimeUnit.SECONDS)
                .followRedirects(false)
                .build();
        Call call = httpClient.newCall(request);
        try {
            ResponseBody response = call.execute().body();
            if (null != response) {
                String body = response.string();
                ResponseSimple resp = JSON.parseObject(body, ResponseSimple.class);
                if (resp.getResultCode() != ResultCodeConst.SUCCESS) {
                    logger.error(resp.getResultMsg());
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    private String handleJvmCmd() {
        //TODO
        console("", "jvm命令开发中...");
        return "";
    }
    private void handleExit() {
        logger.debug("执行exit");
        System.exit(0);
    }
    private String handleSysPropCmd(String key) {
        //TODO 待开发
        return "sysprop命令开发中...";
    }
    private String handleSysEnvCmd(String key) {
        String rlt = "";
        //TODO 待开发
        Map<String, String> envMap = System.getenv();
        if (null == key || key.isEmpty()) {
            //
        }
        String value = envMap.getOrDefault(key, null);
        if (null == value) {
            rlt = String.format("[jarboot]$ Not exist key(%s) in env.", key);
        } else {
            rlt = String.format("[jarboot]$ %s = %s", key, value);
        }
        console("", rlt);
        return "";
    }
    private String handleThread(String tid) {
        //TODO 待开发
        console("", "thread命令开发中...");
        return "";
    }
}
