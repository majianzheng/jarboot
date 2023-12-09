package io.github.majianzheng.jarboot.core.cmd;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.common.notify.NotifyReactor;
import io.github.majianzheng.jarboot.common.protocol.CommandRequest;
import io.github.majianzheng.jarboot.common.protocol.CommandType;
import io.github.majianzheng.jarboot.core.basic.EnvironmentContext;
import io.github.majianzheng.jarboot.core.event.CommandEventBuilder;
import io.github.majianzheng.jarboot.core.event.HeartbeatEvent;
import io.github.majianzheng.jarboot.core.session.AbstractCommandSession;
import io.github.majianzheng.jarboot.core.utils.LogUtils;
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
            session.setRow(request.getRow());
            session.setCol(request.getCol());
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
