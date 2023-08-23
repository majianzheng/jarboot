package io.github.majianzheng.jarboot.client.command;

import io.github.majianzheng.jarboot.client.event.MessageRecvEvent;

import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Command execute future
 * @author majianzheng
 */
@SuppressWarnings("java:S2274")
public class CommandRunFuture implements Future<CommandResult> {
    private final NotifyCallback callback;
    private final CommandCancelable canceler;
    final String cmd;
    final String sid;
    private CommandResult result;
    private boolean canceled = false;
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    CommandRunFuture(String sid, String cmd, NotifyCallback callback, CommandCancelable canceler) {
        this.sid = sid;
        this.cmd = cmd;
        this.callback = callback;
        this.canceler = canceler;
    }

    void finish (boolean success, String msg) {
        if (null == this.result) {
            lock.lock();
            try {
                this.result = new CommandResult(sid, cmd, success, msg);
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    void doCallback(MessageRecvEvent event) {
        if (null != callback) {
            callback.invoke(event);
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (canceled) {
            return true;
        }
        lock.lock();
        try {
            canceled = canceler.invoke(sid, mayInterruptIfRunning);
            if (canceled) {
                if (null == this.result) {
                    this.result = new CommandResult(sid, cmd, true, "Command is canceled");
                }
                condition.signalAll();
            }
        } finally {
            lock.unlock();
        }
        return canceled;
    }

    @Override
    public boolean isCancelled() {
        return this.canceled;
    }

    @Override
    public boolean isDone() {
        return null != this.result || this.canceled;
    }

    @Override
    public CommandResult get() throws InterruptedException, ExecutionException {
        if (null != this.result || this.canceled) {
            return this.result;
        }
        lock.lock();
        try {
            this.condition.await();
        } finally {
            lock.unlock();
        }
        return this.result;
    }

    @Override
    public CommandResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (null != this.result || this.canceled) {
            return this.result;
        }
        lock.lock();
        try {
            if (!this.condition.await(timeout, unit)) {
                throw new TimeoutException("Wait command execute timeout.");
            }
        } finally {
            lock.unlock();
        }
        return this.result;
    }

    interface CommandCancelable {
        /**
         * cancel
         * @param sid service id
         * @param mayInterruptIfRunning may interrupt if running
         * @return success
         */
        boolean invoke(String sid, boolean mayInterruptIfRunning);
    }
}
