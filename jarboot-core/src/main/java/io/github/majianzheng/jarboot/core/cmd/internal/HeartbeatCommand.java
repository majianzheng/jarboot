package io.github.majianzheng.jarboot.core.cmd.internal;


import io.github.majianzheng.jarboot.api.cmd.annotation.Argument;
import io.github.majianzheng.jarboot.common.notify.NotifyReactor;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.core.event.HeartbeatEvent;
import io.github.majianzheng.jarboot.core.stream.ResultStreamDistributor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 心跳
 * @author majianzheng
 */
public class HeartbeatCommand extends AbstractInternalCommand {
    private String sessionIds;

    @Argument(argName = "sessionIds", required=false, index = 0)
    public void setSessionIds(String sessionIds) {
        this.sessionIds = sessionIds;
    }
    @Override
    public void run() {
        if (StringUtils.isNotEmpty(sessionIds)) {
            byte[] buf = Base64.getDecoder().decode(sessionIds.getBytes(StandardCharsets.UTF_8));
            String ids = new String(buf, StandardCharsets.UTF_8);
            ResultStreamDistributor.getInstance().resetActiveSession(ids);
        }
        NotifyReactor.getInstance().publishEvent(new HeartbeatEvent());
    }

    @Override
    public boolean notAllowPublicCall() {
        return true;
    }
}
