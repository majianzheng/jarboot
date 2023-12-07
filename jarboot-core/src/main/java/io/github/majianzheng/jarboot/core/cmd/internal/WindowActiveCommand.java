package io.github.majianzheng.jarboot.core.cmd.internal;

import io.github.majianzheng.jarboot.api.cmd.annotation.Description;
import io.github.majianzheng.jarboot.api.cmd.annotation.Name;
import io.github.majianzheng.jarboot.api.cmd.annotation.Option;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.core.basic.WsClientFactory;
import io.github.majianzheng.jarboot.core.stream.ResultStreamDistributor;

/**
 * 命令取消执行
 * @author majianzheng
 */
@Name("window")
public class WindowActiveCommand extends AbstractInternalCommand {
    private static boolean isFirst = true;
    private boolean active;

    private String host;

    @Option(shortName = "a", longName = "active")
    @Description("window active")
    public void setActive(boolean active) {
        this.active = active;
    }

    @Option(shortName = "h", longName = "host")
    @Description("cluster host")
    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public void run() {
        init();
        if (StringUtils.isNotEmpty(host)) {
            ResultStreamDistributor.getInstance().removeActiveSessionByHost(host);
        }
        if (StringUtils.isEmpty(session.getSessionId())) {
            return;
        }
        if (active) {
            ResultStreamDistributor.getInstance().addActiveSession(session.getSessionId());
        } else {
            ResultStreamDistributor.getInstance().removeActiveSession(session.getSessionId());
        }
    }

    private static void init() {
        if (isFirst) {
            WsClientFactory.getInstance().countDown();
            isFirst = false;
        }
    }

    @Override
    public boolean notAllowPublicCall() {
        return true;
    }
}
