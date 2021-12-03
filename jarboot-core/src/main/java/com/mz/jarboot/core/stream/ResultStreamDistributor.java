package com.mz.jarboot.core.stream;

import com.mz.jarboot.common.CmdProtocol;
import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.ResponseType;
import com.mz.jarboot.core.cmd.model.ResultModel;
import com.mz.jarboot.core.cmd.view.ResultView;
import com.mz.jarboot.core.cmd.view.ResultViewResolver;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.utils.LogUtils;
import org.slf4j.Logger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Use websocket or http to send response data, we need a strategy so that the needed component did not
 * care which to use. The server max socket listen buffer is 8k, we must make sure lower it.
 * @author majianzheng
 */
@SuppressWarnings("all")
public class ResultStreamDistributor {
    private static final Logger logger = LogUtils.getLogger();

    /** flush wait time */
    private static final int WAIT_TIME = 100;
    /** Messge queue */
    private static final ArrayBlockingQueue<CmdProtocol> QUEUE = new ArrayBlockingQueue<>(16384);

    static {
        Thread thread = new Thread(ResultStreamDistributor::consumer);
        thread.setName("jarboot-resp-distributor");
        thread.setDaemon(true);
        thread.start();
    }
    
    private static class ResultStreamDistributorHolder {
        static ResponseStream http = new HttpResponseStreamImpl();
        static ResponseStream socket = new SocketResponseStreamImpl();
        static ResultViewResolver resultViewResolver = new ResultViewResolver();
    }

    /**
     * 输出执行结果
     * @param model   数据
     * @param session 会话
     */
    public static void appendResult(ResultModel model, String session) {
        ResultView resultView = ResultStreamDistributorHolder.resultViewResolver.getResultView(model);
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
     * 发送数据
     * @param resp 数据
     */
    public static void write(CmdProtocol resp) {
        QUEUE.offer(resp);
    }

    private static void consumer() {
        for (; ; ) {
            try {
                CmdProtocol resp = QUEUE.poll(WAIT_TIME, TimeUnit.MILLISECONDS);
                if (null == resp) {
                    StdOutStreamReactor.getInstance().flush();
                } else {
                    sendToServer(resp);
                }
            } catch (Throwable e) {
                //ignore
            }
        }
    }

    private static void sendToServer(CmdProtocol resp) {
        //根据数据包的大小选择合适的通讯方式
        String raw = resp.toRaw();
        ResponseStream stream = (raw.length() < CoreConstant.SOCKET_MAX_SEND) ?
                ResultStreamDistributorHolder.socket : ResultStreamDistributorHolder.http;
        stream.write(raw);
    }

    private ResultStreamDistributor() {}
}
