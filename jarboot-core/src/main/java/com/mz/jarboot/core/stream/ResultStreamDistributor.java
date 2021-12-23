package com.mz.jarboot.core.stream;

import com.mz.jarboot.common.protocol.CmdProtocol;
import com.mz.jarboot.common.protocol.CommandConst;
import com.mz.jarboot.common.protocol.CommandResponse;
import com.mz.jarboot.common.protocol.ResponseType;
import com.mz.jarboot.core.basic.WsClientFactory;
import com.mz.jarboot.core.cmd.model.ResultModel;
import com.mz.jarboot.core.cmd.view.ResultView;
import com.mz.jarboot.core.cmd.view.ResultViewResolver;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.utils.LogUtils;
import com.mz.jarboot.core.utils.StringUtils;
import org.slf4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Use websocket or http to send response data, we need a strategy so that the needed component did not
 * care which to use. The server max socket listen buffer is 8k, we must make sure lower it.
 * @author majianzheng
 */
public class ResultStreamDistributor extends Thread {
    private static final Logger logger = LogUtils.getLogger();

    /** Message queue */
    private final LinkedBlockingQueue<CmdProtocol> queue = new LinkedBlockingQueue<>(16384);
    private final ResponseStream http = new HttpResponseStreamImpl();
    private final ResponseStream socket = new SocketResponseStreamImpl();
    private final ResultViewResolver resultViewResolver = new ResultViewResolver();

    /** instance holder */
    private static class ResultStreamDistributorHolder {
        static final ResultStreamDistributor INST = new ResultStreamDistributor();
    }

    /**
     * 输出执行结果
     * @param model   数据
     * @param session 会话
     */
    @SuppressWarnings("all")
    public static void appendResult(ResultModel model, String session) {
        ResultView resultView = ResultStreamDistributorHolder.INST.resultViewResolver.getResultView(model);
        if (resultView == null) {
            logger.info("获取视图解析失败！{}, {}", model.getName(), model.getClass());
            return;
        }
        String text = resultView.render(model);
        CommandResponse resp = new CommandResponse();
        resp.setSuccess(true);
        resp.setResponseType(resultView.isJson() ? ResponseType.JSON_RESULT : ResponseType.CONSOLE);
        resp.setBody(text);
        resp.setSessionId(session);
        write(resp);
    }

    /**
     * 标准输出，忽略格式，html代码会以纯文本的形式打印出来
     * @param text 文本
     */
    public static void stdPrint(String text) {
        if (StringUtils.isEmpty(text)) {
            return;
        }
        CommandResponse resp = new CommandResponse();
        resp.setSuccess(true);
        resp.setResponseType(ResponseType.STD_PRINT);
        resp.setBody(text);
        resp.setSessionId(CommandConst.SESSION_COMMON);
        write(resp);
    }

    /**
     * 标准输出，退格
     * @param num 次数
     */
    public static void stdBackspace(int num) {
        if (num > 0) {
            CommandResponse resp = new CommandResponse();
            resp.setSuccess(true);
            resp.setResponseType(ResponseType.BACKSPACE);
            resp.setBody(String.valueOf(num));
            resp.setSessionId(CommandConst.SESSION_COMMON);
            write(resp);
        }
    }

    /**
     * 发送数据
     * @param resp 数据
     */
    @SuppressWarnings("all")
    public static void write(CmdProtocol resp) {
        ResultStreamDistributorHolder.INST.queue.offer(resp);
    }

    @Override
    public void run() {
        for (; ; ) {
            try {
                CmdProtocol resp = queue.take();
                sendToServer(resp);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                //ignore
            }
        }
    }

    private static void sendToServer(CmdProtocol resp) {
        if (WsClientFactory.getInstance().isOnline()) {
            //根据数据包的大小选择合适的通讯方式
            String raw = resp.toRaw();
            ResponseStream stream = (raw.length() < CoreConstant.SOCKET_MAX_SEND) ?
                    ResultStreamDistributorHolder.INST.socket : ResultStreamDistributorHolder.INST.http;
            stream.write(raw);
        }
    }

    private ResultStreamDistributor() {
        this.setDaemon(true);
        this.setName("jarboot.stream");
        this.start();
    }
}
