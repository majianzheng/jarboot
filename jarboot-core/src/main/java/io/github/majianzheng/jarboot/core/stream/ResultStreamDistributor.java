package io.github.majianzheng.jarboot.core.stream;

import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.common.notify.NotifyReactor;
import io.github.majianzheng.jarboot.common.protocol.CommandResponse;
import io.github.majianzheng.jarboot.common.protocol.NotifyType;
import io.github.majianzheng.jarboot.common.protocol.ResponseType;
import io.github.majianzheng.jarboot.core.basic.WsClientFactory;
import io.github.majianzheng.jarboot.core.cmd.model.ResultModel;
import io.github.majianzheng.jarboot.core.cmd.view.ResultView;
import io.github.majianzheng.jarboot.core.cmd.view.ResultViewResolver;
import io.github.majianzheng.jarboot.core.event.ResponseEventBuilder;
import io.github.majianzheng.jarboot.core.event.StdoutAppendEvent;
import io.github.majianzheng.jarboot.core.utils.LogUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import org.slf4j.Logger;

import java.util.*;

/**
 * Use websocket or http to send response data, we need a strategy so that the needed component did not
 * care which to use. The server max socket listen buffer is 8k, we must make sure lower it.
 * @author majianzheng
 */
public class ResultStreamDistributor {
    private static final Logger logger = LogUtils.getLogger();

    private final ResponseStream stream = new ResponseStreamDelegate();
    private final ResultViewResolver resultViewResolver = new ResultViewResolver();
    private Set<String> stdoutSessionActiveSet = new HashSet<>(16);

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

    public void removeActiveSessionByHost(String host) {
        List<String> waitDelete = new ArrayList<>();
        stdoutSessionActiveSet.forEach(s -> {
            if (s.startsWith(host)) {
                waitDelete.add(s);
            }
        });
        waitDelete.forEach(stdoutSessionActiveSet::remove);
    }

    public void addActiveSession(String sessionId) {
        if (StringUtils.isEmpty(sessionId)) {
            return;
        }
        Set<String> copy = new HashSet<>(stdoutSessionActiveSet);
        String[] session = sessionId.split(",");
        copy.addAll(Arrays.asList(session));
        stdoutSessionActiveSet = copy;
    }

    public void removeActiveSession(String sessionId) {
        if (StringUtils.isEmpty(sessionId)) {
            return;
        }
        Set<String> copy = new HashSet<>(stdoutSessionActiveSet);
        String[] session = sessionId.split(",");
        for (String s : session) {
            copy.remove(s);
        }
        stdoutSessionActiveSet = copy;
    }

    public void resetActiveSession(String sessionIds) {
        if (StringUtils.isEmpty(sessionIds)) {
            stdoutSessionActiveSet = new HashSet<>(16);
            return;
        }
        String[] session = sessionIds.split(",");
        stdoutSessionActiveSet = new HashSet<>(Arrays.asList(session));
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
                if (stdoutSessionActiveSet.isEmpty()) {
                    return;
                }
                String session = String.join(",", stdoutSessionActiveSet);
                sendToServer(new ResponseEventBuilder()
                        .success(true)
                        .type(ResponseType.STD_PRINT)
                        .body(event.getText())
                        .session(session)
                        .build());
            }

            @Override
            public Class<? extends JarbootEvent> subscribeType() {
                return StdoutAppendEvent.class;
            }
        });
    }
}
