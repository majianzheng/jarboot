package com.mz.jarboot.core.cmd.internal;


import com.mz.jarboot.core.basic.WsClientFactory;

/**
 * 心跳
 * @author majianzheng
 */
public class HeartbeatCommand extends AbstractInternalCommand {
    @Override
    public void run() {
        WsClientFactory.getInstance().onHeartbeat();
        session.end(true, "check heartbeat success!");
    }
}
