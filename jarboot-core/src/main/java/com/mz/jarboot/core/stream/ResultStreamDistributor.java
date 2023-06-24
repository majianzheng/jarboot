package com.mz.jarboot.core.stream;

import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.common.notify.NotifyReactor;
import com.mz.jarboot.common.protocol.CommandResponse;
import com.mz.jarboot.common.protocol.NotifyType;
import com.mz.jarboot.common.protocol.ResponseType;
import com.mz.jarboot.core.basic.WsClientFactory;
import com.mz.jarboot.core.cmd.model.ResultModel;
import com.mz.jarboot.core.cmd.view.ResultView;
import com.mz.jarboot.core.cmd.view.ResultViewResolver;
import com.mz.jarboot.core.event.ResponseEventBuilder;
import com.mz.jarboot.core.event.StdoutAppendEvent;
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

    private final ResponseStream stream = new ResponseStreamDelegate();
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
     * @param session 会话
     * @param model   数据
     */
    @SuppressWarnings({"unchecked", "java:S3740", "rawtypes"})
    public void appendResult(CommandSession session, ResultModel model) {
        ResultView resultView = ResultStreamDistributorHolder.INST.resultViewResolver.getResultView(model);
        if (resultView == null) {
            logger.info("获取视图解析失败！{}, {}", model.getName(), model.getClass());
            return;
        }
        String text = resultView.render(session, model);
        NotifyType type = resultView.isJson() ? NotifyType.JSON_RESULT : NotifyType.CONSOLE;
        response(true, ResponseType.NOTIFY, type.body(text), session.getSessionId());
    }

    /**
     * 分布式日志记录
     * @param text 日志
     */
    public void log(String text) {
        response(true, ResponseType.LOG_APPENDER, text, StringUtils.EMPTY);
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
            stream.write(raw);
        }
    }

    private ResultStreamDistributor() {
        //命令响应事件
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
        //std文本输出事件
        NotifyReactor.getInstance().registerSubscriber(new Subscriber<StdoutAppendEvent>() {
            @Override
            public void onEvent(StdoutAppendEvent event) {
                sendToServer(new ResponseEventBuilder()
                        .success(true)
                        .type(ResponseType.STD_PRINT)
                        .body(event.getText())
                        .session(StringUtils.EMPTY)
                        .build());
            }

            @Override
            public Class<? extends JarbootEvent> subscribeType() {
                return StdoutAppendEvent.class;
            }
        });
    }
}
