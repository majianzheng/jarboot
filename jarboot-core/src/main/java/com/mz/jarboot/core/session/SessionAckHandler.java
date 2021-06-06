package com.mz.jarboot.core.session;

public interface SessionAckHandler {
    void ack(String message);
}
