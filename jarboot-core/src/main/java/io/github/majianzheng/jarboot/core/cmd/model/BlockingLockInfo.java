package io.github.majianzheng.jarboot.core.cmd.model;

import java.lang.management.ThreadInfo;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class BlockingLockInfo {

    /** the thread info that is holing this lock. */
    private ThreadInfo threadInfo = null;
    /** the associated LockInfo object */
    private int lockIdentityHashCode = 0;
    /** the number of thread that is blocked on this lock */
    private int blockingThreadCount = 0;

    public BlockingLockInfo() {
        //do nothing
    }

    public ThreadInfo getThreadInfo() {
        return threadInfo;
    }

    public void setThreadInfo(ThreadInfo threadInfo) {
        this.threadInfo = threadInfo;
    }

    public int getLockIdentityHashCode() {
        return lockIdentityHashCode;
    }

    public void setLockIdentityHashCode(int lockIdentityHashCode) {
        this.lockIdentityHashCode = lockIdentityHashCode;
    }

    public int getBlockingThreadCount() {
        return blockingThreadCount;
    }

    public void setBlockingThreadCount(int blockingThreadCount) {
        this.blockingThreadCount = blockingThreadCount;
    }
}
