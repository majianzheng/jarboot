package com.mz.jarboot.event;

import org.springframework.context.ApplicationContext;

public class ContextEventPub {
    private static volatile ContextEventPub instance = null;
    private ApplicationContext ctx; //应用上下文
    private ContextEventPub() {}

    public static ContextEventPub getInstance() {
        if (null == instance) {
            synchronized (ContextEventPub.class) {
                instance = new ContextEventPub();
            }
        }
        return instance;
    }

    public void publish(Object event) {
        if (null != ctx) {
            ctx.publishEvent(event);
        }
    }

    public ApplicationContext getContext() {
        return this.ctx;
    }

    public void setContext(final ApplicationContext ctx) {
        this.ctx = ctx;
    }
}
