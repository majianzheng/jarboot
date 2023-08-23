package io.github.majianzheng.jarboot.core.cmd.impl;

import io.github.majianzheng.jarboot.api.cmd.annotation.*;
import io.github.majianzheng.jarboot.core.cmd.AbstractCommand;
import io.github.majianzheng.jarboot.core.cmd.model.BlockingLockInfo;
import io.github.majianzheng.jarboot.core.cmd.model.BusyThreadInfo;
import io.github.majianzheng.jarboot.core.cmd.model.ThreadModel;
import io.github.majianzheng.jarboot.core.cmd.model.ThreadVO;
import io.github.majianzheng.jarboot.core.constant.CoreConstant;
import io.github.majianzheng.jarboot.core.utils.ArrayUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.core.utils.ThreadUtil;

import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
@Name("thread")
@Summary("Display thread info, thread stack")
@Description(CoreConstant.EXAMPLE +
        "  thread\n" +
        "  thread 51\n" +
        "  thread -n -1\n" +
        "  thread -n 5\n" +
        "  thread -b\n" +
        "  thread -i 2000\n" +
        "  thread --state BLOCKED\n" +
        CoreConstant.WIKI + CoreConstant.WIKI_HOME + "thread")
public class ThreadCommand extends AbstractCommand {
    private static Set<String> states = null;
    private static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private long id = -1;
    private Integer topNBusy = null;
    private boolean findMostBlockingThread = false;
    private int sampleInterval = 200;
    private String state;

    private boolean lockedMonitors = false;
    private boolean lockedSynchronizers = false;
    private boolean all = false;

    static {
        states = new HashSet<>(State.values().length);
        for (State state : State.values()) {
            states.add(state.name());
        }
    }
    @Argument(index = 0, required = false, argName = "id")
    @Description("Show thread stack")
    public void setId(long id) {
        this.id = id;
    }

    @Option(longName = "all", flag = true)
    @Description("Display all thread results instead of the first page")
    public void setAll(boolean all) {
        this.all = all;
    }

    @Option(shortName = "n", longName = "top-n-threads")
    @Description("The number of thread(s) to show, ordered by cpu utilization, -1 to show all.")
    public void setTopNBusy(Integer topNBusy) {
        this.topNBusy = topNBusy;
    }

    @Option(shortName = "b", longName = "include-blocking-thread", flag = true)
    @Description("Find the thread who is holding a lock that blocks the most number of threads.")
    public void setFindMostBlockingThread(boolean findMostBlockingThread) {
        this.findMostBlockingThread = findMostBlockingThread;
    }

    @Option(shortName = "i", longName = "sample-interval")
    @Description("Specify the sampling interval (in ms) when calculating cpu usage.")
    public void setSampleInterval(int sampleInterval) {
        this.sampleInterval = sampleInterval;
    }

    @Option(longName = "state")
    @Description("Display the thead filter by the state. NEW, RUNNABLE, TIMED_WAITING, WAITING, BLOCKED, TERMINATED is optional.")
    public void setState(String state) {
        this.state = state;
    }

    @Option(longName = "lockedMonitors", flag = true)
    @Description("Find the thread info with lockedMonitors flag, default value is false.")
    public void setLockedMonitors(boolean lockedMonitors) {
        this.lockedMonitors = lockedMonitors;
    }

    @Option(longName = "lockedSynchronizers", flag = true)
    @Description("Find the thread info with lockedSynchronizers flag, default value is false.")
    public void setLockedSynchronizers(boolean lockedSynchronizers) {
        this.lockedSynchronizers = lockedSynchronizers;
    }

    @Override
    public void run() {
        if (id > 0) {
            processThread();
        } else if (topNBusy != null) {
            processTopBusyThreads();
        } else if (findMostBlockingThread) {
            processBlockingThread();
        } else {
            processAllThreads();
        }
    }

    private void processAllThreads() {
        List<ThreadVO> threads = ThreadUtil.getThreads();

        // 统计各种线程状态
        Map<State, Integer> stateCountMap = new LinkedHashMap<>();
        for (State s : State.values()) {
            stateCountMap.put(s, 0);
        }

        for (ThreadVO thread : threads) {
            State threadState = thread.getState();
            Integer count = stateCountMap.get(threadState);
            stateCountMap.put(threadState, count + 1);
        }

        boolean includeInternalThreads = true;
        Collection<ThreadVO> resultThreads = new ArrayList<>();
        if (!StringUtils.isEmpty(this.state)) {
            this.state = this.state.toUpperCase();
            if (states.contains(this.state)) {
                includeInternalThreads = false;
                for (ThreadVO thread : threads) {
                    if (thread.getState() != null && state.equals(thread.getState().name())) {
                        resultThreads.add(thread);
                    }
                }
            } else {
                session.end(false, "Illegal argument, state should be one of " + states);
                return;
            }
        } else {
            resultThreads = threads;
        }

        //thread stats
        ThreadSampler threadSampler = new ThreadSampler();
        threadSampler.setIncludeInternalThreads(includeInternalThreads);
        threadSampler.sample(resultThreads);
        threadSampler.pause(sampleInterval);
        List<ThreadVO> threadStats = threadSampler.sample(resultThreads);

        session.appendResult(new ThreadModel(threadStats, stateCountMap, all));
        session.end();
    }

    private void processBlockingThread() {
        BlockingLockInfo blockingLockInfo = ThreadUtil.findMostBlockingLock();
        if (blockingLockInfo.getThreadInfo() == null) {
            session.end(false, "No most blocking thread found!");
            return;
        }
        session.appendResult(new ThreadModel(blockingLockInfo));
        session.end();
    }

    private void processTopBusyThreads() {
        ThreadSampler threadSampler = new ThreadSampler();
        threadSampler.sample(ThreadUtil.getThreads());
        threadSampler.pause(sampleInterval);
        List<ThreadVO> threadStats = threadSampler.sample(ThreadUtil.getThreads());

        int limit = Math.min(threadStats.size(), topNBusy);

        List<ThreadVO> topNThreads = null;
        if (limit > 0) {
            topNThreads = threadStats.subList(0, limit);
        } else { // -1 for all threads
            topNThreads = threadStats;
        }

        List<Long> tids = new ArrayList<>(topNThreads.size());
        for (ThreadVO thread : topNThreads) {
            if (thread.getId() > 0) {
                tids.add(thread.getId());
            }
        }

        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(ArrayUtils.toPrimitive(tids.toArray(new Long[0])), lockedMonitors, lockedSynchronizers);
        if (!tids.isEmpty() && threadInfos == null) {
            session.end(false, "get top busy threads failed");
            return;
        }

        //threadInfo with cpuUsage
        List<BusyThreadInfo> busyThreadInfos = new ArrayList<>(topNThreads.size());
        for (ThreadVO thread : topNThreads) {
            ThreadInfo threadInfo = findThreadInfoById(threadInfos, thread.getId());
            BusyThreadInfo busyThread = new BusyThreadInfo(thread, threadInfo);
            busyThreadInfos.add(busyThread);
        }
        session.appendResult(new ThreadModel(busyThreadInfos));
        session.end();
    }

    private ThreadInfo findThreadInfoById(ThreadInfo[] threadInfos, long id) {
        for (int i = 0; i < threadInfos.length; i++) {
            ThreadInfo threadInfo = threadInfos[i];
            if ( threadInfo.getThreadId() == id) {
                return threadInfo;
            }
        }
        return null;
    }

    private void processThread() {
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(new long[]{id}, lockedMonitors, lockedSynchronizers);
        if (threadInfos == null || threadInfos.length < 1 || threadInfos[0] == null) {
            session.end(false, "thread do not exist! id: " + id);
            return;
        }

        session.appendResult(new ThreadModel(threadInfos[0]));
        session.end();
    }
}
