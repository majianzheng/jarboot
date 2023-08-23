package io.github.majianzheng.jarboot.core.stream;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.common.utils.ApiStringBuilder;
import io.github.majianzheng.jarboot.core.basic.EnvironmentContext;
import io.github.majianzheng.jarboot.common.utils.HttpUtils;
import org.apache.http.entity.ByteArrayEntity;

/**
 * 大数据量传输通过http协议，使用WebSocket会增加额外的拆包、组包实现增加业务复杂性
 * 快慢个几毫秒眼睛也分辨不出来
 * @author majianzheng
 */
public class HttpResponseStreamImpl implements ResponseStream {
    private static final String API = CommonConst.AGENT_CLIENT_CONTEXT + "/response";

    @Override
    public void write(byte[] data) {
        final String url = EnvironmentContext.getBaseUrl() + new ApiStringBuilder(API)
                .add(CommonConst.SERVICE_NAME_PARAM, EnvironmentContext.getAgentClient().getServiceName())
                .add(CommonConst.SID_PARAM, EnvironmentContext.getAgentClient().getSid())
                .add(CommonConst.USER_DIR, EnvironmentContext.getAgentClient().getUserDir())
                .build();
        HttpUtils.doPost(url, new ByteArrayEntity(data), HttpUtils.CONTENT_TYPE_JSON, null);
    }
}
