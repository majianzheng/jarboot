package io.github.majianzheng.jarboot.common.notify;

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
        for (; ; ) {
            try {
                loop();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Throwable ex) {
                //ignore
            }
        }
    }

    /**
     * 事件循环
     * @throws InterruptedException
     */
    protected abstract void loop() throws InterruptedException;
}
