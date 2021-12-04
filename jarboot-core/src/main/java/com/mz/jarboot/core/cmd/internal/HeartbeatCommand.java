package com.mz.jarboot.core.cmd.internal;


/**
 * 心跳
 * @author majianzheng
 */
public class HeartbeatCommand extends AbstractInternalCommand {
    @Override
    public void run() {
        session.end(true, "heartbeat success");
    }
}
