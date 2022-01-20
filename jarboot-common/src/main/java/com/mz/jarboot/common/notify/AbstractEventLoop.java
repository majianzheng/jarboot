package com.mz.jarboot.common.notify;

/**
 * @author majianzheng
 */
@SuppressWarnings({ "squid:S1181", "ConditionalBreakInInfiniteLoop" })
public abstract class AbstractEventLoop extends Thread {
    private volatile boolean initialized = false;
    protected volatile boolean shutdown = false;

    protected AbstractEventLoop() {
        this("jarboot.event.loop");
    }

    protected AbstractEventLoop(String name) {
        setDaemon(true);
        setName(name);
        start();
    }

    @Override
    public synchronized void start() {
        if (!initialized) {
            // start just called once
            super.start();
            initialized = true;
        }
    }

    @Override
    public final void run() {
        try {
            for (; ; ) {
                loop();
                if (shutdown) {
                    break;
                }
            }
        } catch (Throwable ex) {
            //ignore
        }
    }

    /**
     * 事件循环
     */
    protected abstract void loop();
}
