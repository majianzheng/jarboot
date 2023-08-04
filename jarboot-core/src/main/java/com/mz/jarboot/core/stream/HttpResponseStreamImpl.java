package com.mz.jarboot.core.stream;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.common.utils.ApiStringBuilder;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.utils.HttpUtils;

/**
 * 大数据量传输通过http协议，使用WebSocket会增加额外的拆包、组包实现增加业务复杂性
 * 快慢个几毫秒眼睛也分辨不出来
 * @author majianzheng
 */
public class HttpResponseStreamImpl implements ResponseStream {
    private static final String API = CommonConst.AGENT_CLIENT_CONTEXT + "/response";

    @Override
    public void write(byte[] data) {
        final String url = new ApiStringBuilder(API)
                .add(CommonConst.SERVICE_NAME_PARAM, EnvironmentContext.getAgentClient().getServiceName())
                .add(CommonConst.SID_PARAM, EnvironmentContext.getAgentClient().getSid())
                .add(CommonConst.USER_DIR, EnvironmentContext.getAgentClient().getUserDir())
                .build();
        HttpUtils.postSimple(url, data);
    }
}
