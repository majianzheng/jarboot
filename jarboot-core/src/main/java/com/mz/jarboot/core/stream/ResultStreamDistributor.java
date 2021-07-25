package com.mz.jarboot.core.stream;

import com.mz.jarboot.common.CmdProtocol;
import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.ResponseType;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.cmd.model.ResultModel;
import com.mz.jarboot.core.cmd.view.ResultView;
import com.mz.jarboot.core.cmd.view.ResultViewResolver;
import com.mz.jarboot.core.constant.CoreConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use websocket or http to send response data, we need a strategy so that the needed component did not
 * care which to use. The server max socket listen buffer is 8k, we must make sure lower it.
 * @author majianzheng
 */
@SuppressWarnings("all")
public class ResultStreamDistributor {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private final ResponseStream http = new HttpResponseStreamImpl();
    private final ResponseStream socket = new SocketResponseStreamImpl();
    private final ResultViewResolver resultViewResolver = EnvironmentContext.getResultViewResolver();
    private final String session;
    public ResultStreamDistributor(String session) {
        this.session = session;
    }

    public void appendResult(ResultModel model) {
        ResultView resultView = resultViewResolver.getResultView(model);
        if (resultView == null) {
            logger.info("获取视图解析失败！{}, {}", model.getName(), model.getClass());
            return;
        }
        String text = resultView.render(model);
        CommandResponse resp = new CommandResponse();
        resp.setSuccess(true);
        resp.setResponseType(resultView.isJson() ? ResponseType.JSON_RESULT : ResponseType.CONSOLE);
        resp.setBody(text);
        resp.setSessionId(this.session);
        this.write(resp);
    }

    public void write(CmdProtocol resp) {
        //根据数据包的大小选择合适的通讯方式
        String raw = resp.toRaw();
        if (raw.length() < CoreConstant.SOCKET_MAX_SEND) {
            socket.write(raw);
        } else {
            http.write(raw);
        }
    }
}
