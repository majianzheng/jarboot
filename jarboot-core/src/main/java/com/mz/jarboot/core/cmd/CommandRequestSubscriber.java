package com.mz.jarboot.core.cmd;

import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.common.notify.NotifyReactor;
import com.mz.jarboot.common.protocol.CommandRequest;
import com.mz.jarboot.common.protocol.CommandType;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.event.CommandEventBuilder;
import com.mz.jarboot.core.event.HeartbeatEvent;
import com.mz.jarboot.core.session.AbstractCommandSession;
import com.mz.jarboot.core.utils.LogUtils;
import org.slf4j.Logger;

/**
 * Command dispatch, the main loop of the logic.
 * @author majianzheng
 */
public class CommandRequestSubscriber implements Subscriber<CommandRequest> {
    private final Logger logger = LogUtils.getLogger();

    @Override
    public void onEvent(CommandRequest request) {
        AbstractCommandSession session = null;
        try {
            session = EnvironmentContext.registerSession(request.getSessionId());
            CommandType type = request.getCommandType();
            if (CommandType.HEARTBEAT.equals(type)) {
                NotifyReactor.getInstance().publishEvent(new HeartbeatEvent(request));
                return;
            }
            JarbootEvent event = new CommandEventBuilder()
                    .request(request)
                    .session(session)
                    .build();
            if (null != event) {
                NotifyReactor.getInstance().publishEvent(event);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            if (null != session) {
                session.end();
            }
        }
    }

    @Override
    public Class<? extends JarbootEvent> subscribeType() {
        return CommandRequest.class;
    }
}
