package com.mz.jarboot.core.stream;

import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.common.notify.NotifyReactor;
import com.mz.jarboot.common.protocol.CommandConst;
import com.mz.jarboot.common.protocol.CommandResponse;
import com.mz.jarboot.common.protocol.ResponseType;
import com.mz.jarboot.core.basic.WsClientFactory;
import com.mz.jarboot.core.cmd.model.ResultModel;
import com.mz.jarboot.core.cmd.view.ResultView;
import com.mz.jarboot.core.cmd.view.ResultViewResolver;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.event.ResponseEventBuilder;
import com.mz.jarboot.core.utils.LogUtils;
import com.mz.jarboot.common.utils.StringUtils;
import org.slf4j.Logger;

/**
 * Use websocket or http to send response data, we need a strategy so that the needed component did not
 * care which to use. The server max socket listen buffer is 8k, we must make sure lower it.
 * @author majianzheng
 */
public class ResultStreamDistributor {
    private static final Logger logger = LogUtils.getLogger();

    private final ResponseStream http = new HttpResponseStreamImpl();
    private final ResponseStream socket = new SocketResponseStreamImpl();
    private final ResultViewResolver resultViewResolver = new ResultViewResolver();

    public static ResultStreamDistributor getInstance() {
        return ResultStreamDistributorHolder.INST;
    }

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
    public void appendResult(ResultModel model, String session) {
        ResultView resultView = ResultStreamDistributorHolder.INST.resultViewResolver.getResultView(model);
        if (resultView == null) {
            logger.info("获取视图解析失败！{}, {}", model.getName(), model.getClass());
            return;
        }
        String text = resultView.render(model);
        ResponseType type = resultView.isJson() ? ResponseType.JSON_RESULT : ResponseType.CONSOLE;
        response(true, type, text, session);
    }

    /**
     * 标准输出，忽略格式，html代码会以纯文本的形式打印出来
     * @param text 文本
     */
    public void stdPrint(String text) {
        if (StringUtils.isEmpty(text)) {
            return;
        }
        response(true, ResponseType.STD_PRINT, text, CommandConst.SESSION_COMMON);
    }

    /**
     * 标准输出，退格
     * @param num 次数
     */
    public void stdBackspace(int num) {
        if (num > 0) {
            response(true, ResponseType.BACKSPACE, String.valueOf(num), CommandConst.SESSION_COMMON);
        }
    }

    /**
     * 分布式日志记录
     * @param text 日志
     */
    public void log(String text) {
        response(true, ResponseType.LOG_APPENDER, text, CommandConst.SESSION_COMMON);
    }

    public void response(boolean success, ResponseType type, String body, String id) {
        NotifyReactor
                .getInstance()
                .publishEvent(new ResponseEventBuilder()
                        .success(success)
                        .type(type)
                        .body(body)
                        .session(id)
                        .build());
    }

    private void sendToServer(CommandResponse resp) {
        if (WsClientFactory.getInstance().isOnline()) {
            //根据数据包的大小选择合适的通讯方式
            byte[] raw = resp.toRaw();
            ResponseStream stream = (raw.length < CoreConstant.SOCKET_MAX_SEND) ? socket : http;
            stream.write(raw);
        }
    }

    private ResultStreamDistributor() {
        NotifyReactor.getInstance().registerSubscriber(new Subscriber<CommandResponse>() {
            @Override
            public void onEvent(CommandResponse event) {
                sendToServer(event);
            }

            @Override
            public Class<? extends JarbootEvent> subscribeType() {
                return CommandResponse.class;
            }
        });
    }
}
