package com.mz.jarboot.core.resp;

import com.mz.jarboot.common.CmdProtocol;
import com.mz.jarboot.core.constant.CoreConstant;

/**
 * Use websocket or http to send response data, we need a strategy so that the needed component did not
 * care which to use. The server max socket listen buffer is 8k, we must make sure lower it.
 * @author jianzhengma
 */
public class RespStreamStrategy {
    private ResponseStream http = new HttpResponseStreamImpl();
    private ResponseStream socket = new SocketResponseStreamImpl();

    public void write(CmdProtocol resp) {
        String raw = resp.toRaw();
        if (raw.length() < CoreConstant.SOCKET_MAX_SEND) {
            socket.write(raw);
        } else {
            http.write(raw);
        }
    }
}
