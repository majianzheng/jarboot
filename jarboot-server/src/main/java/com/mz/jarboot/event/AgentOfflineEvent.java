package com.mz.jarboot.event;

/**
 * Spring Application Event will published when the agent task offline
 * {@link org.springframework.context.event.EventListener}
 * @author majianzheng
 */
public final class AgentOfflineEvent {
    private final String server;
    private final long offlineTime;

    public AgentOfflineEvent(String server) {
        this.server = server;
        offlineTime = System.currentTimeMillis();
    }

    public String getServer() {
        return server;
    }

    public long getOfflineTime() {
        return offlineTime;
    }
}
