package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.core.cmd.model.BusyThreadInfo;
import com.mz.jarboot.core.cmd.model.ThreadModel;
import com.mz.jarboot.core.cmd.model.ThreadVO;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.core.utils.ThreadUtil;

import java.util.List;
import java.util.Map;


/**
 * View of 'thread' command
 *
 * @author majianzheng
 */
public class ThreadView implements ResultView<ThreadModel> {

    @SuppressWarnings("java:S3776")
    @Override
    public String render(ThreadModel result) {
        if (result.getThreadInfo() != null) {
            // no cpu usage info
            return ThreadUtil.getFullStacktrace(result.getThreadInfo());
        } else if (result.getBusyThreads() != null) {
            List<BusyThreadInfo> threadInfos = result.getBusyThreads();
            StringBuilder sb = new StringBuilder();
            for (BusyThreadInfo info : threadInfos) {
                String stacktrace = ThreadUtil.getFullStacktrace(info, -1, -1);
                sb.append(stacktrace).append(StringUtils.LF);
            }
            return sb.toString();
        } else if (result.getBlockingLockInfo() != null) {
            return ThreadUtil.getFullStacktrace(result.getBlockingLockInfo());
        } else if (result.getThreadStateCount() != null) {
            Map<Thread.State, Integer> threadStateCount = result.getThreadStateCount();
            List<ThreadVO> threadStats = result.getThreadStats();

            //sum total thread count
            int total = 0;
            for (Integer value : threadStateCount.values()) {
                total += value;
            }

            int internalThreadCount = 0;
            for (ThreadVO thread : threadStats) {
                if (thread.getId() <= 0) {
                    internalThreadCount += 1;
                }
            }
            total += internalThreadCount;

            StringBuilder threadStat = new StringBuilder();
            threadStat.append("Threads Total: ").append(total);

            for (Thread.State s : Thread.State.values()) {
                Integer count = threadStateCount.get(s);
                threadStat.append(", ").append(s.name()).append(": ").append(count);
            }
            if (internalThreadCount > 0) {
                threadStat.append(", Internal threads: ").append(internalThreadCount);
            }
            String stat = threadStat.toString();

            //thread stats
            int height;
            if (result.isAll()) {
                height = threadStats.size() + 1;
            } else {
                //remove blank lines
                height = Math.min(32, threadStats.size() + 2);
            }
            String content = ViewRenderUtil.drawThreadInfo(threadStats, height);
            return stat + content;
        }
        return StringUtils.EMPTY;
    }
}
