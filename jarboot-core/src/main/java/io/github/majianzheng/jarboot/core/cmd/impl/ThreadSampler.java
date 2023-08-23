package io.github.majianzheng.jarboot.core.cmd.impl;

import io.github.majianzheng.jarboot.core.cmd.model.ThreadVO;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;

/**
 * Thread cpu sampler
 *
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class ThreadSampler {

    private static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    private Map<ThreadVO, Long> lastCpuTimes = new HashMap<>();

    private long lastSampleTimeNanos;
    private boolean includeInternalThreads = true;


    @SuppressWarnings("java:S3776")
    public List<ThreadVO> sample(Collection<ThreadVO> originThreads) {

        List<ThreadVO> threads = new ArrayList<>(originThreads);

        // Sample CPU
        if (lastCpuTimes.isEmpty()) {
            lastSampleTimeNanos = System.nanoTime();
            for (ThreadVO thread : threads) {
                if (thread.getId() > 0) {
                    long cpu = threadMXBean.getThreadCpuTime(thread.getId());
                    lastCpuTimes.put(thread, cpu);
                    thread.setTime(cpu / 1000000);
                }
            }

            //sort by time
            threads.sort((o1, o2) -> {
                long l1 = o1.getTime();
                long l2 = o2.getTime();
                return Long.compare(l2, l1);
            });
            return threads;
        }

        // Resample
        long newSampleTimeNanos = System.nanoTime();
        Map<ThreadVO, Long> newCpuTimes = new HashMap<>(threads.size());
        for (ThreadVO thread : threads) {
            if (thread.getId() > 0) {
                long cpu = threadMXBean.getThreadCpuTime(thread.getId());
                newCpuTimes.put(thread, cpu);
            }
        }

        // Compute delta time
        final Map<ThreadVO, Long> deltas = new HashMap<>(threads.size());
        newCpuTimes.forEach((thread, v) -> {
            Long t = lastCpuTimes.get(thread);
            if (t == null) {
                t = 0L;
            }
            long time1 = t;
            long time2 = newCpuTimes.get(thread);
            if (time1 == -1) {
                time1 = time2;
            } else if (time2 == -1) {
                time2 = time1;
            }
            long delta = time2 - time1;
            deltas.put(thread, delta);
        });

        long sampleIntervalNanos = newSampleTimeNanos - lastSampleTimeNanos;

        // Compute cpu usage
        final HashMap<ThreadVO, Double> cpuUsages = new HashMap<>(threads.size());
        for (ThreadVO thread : threads) {
            double cpu = sampleIntervalNanos == 0 ? 0.0 : (deltas.get(thread) * 10000 / sampleIntervalNanos / 100.0);
            cpuUsages.put(thread, cpu);
        }

        // Sort by CPU time : should be a rendering hint...
        threads.sort((o1, o2) -> {
            long l1 = deltas.get(o1);
            long l2 = deltas.get(o2);
            return Long.compare(l2, l1);
        });

        for (ThreadVO thread : threads) {
            //nanos to mills
            long timeMills = newCpuTimes.get(thread) / 1000000;
            long deltaTime = deltas.get(thread) / 1000000;
            double cpu = cpuUsages.get(thread);

            thread.setCpu(cpu);
            thread.setTime(timeMills);
            thread.setDeltaTime(deltaTime);
        }
        lastCpuTimes = newCpuTimes;
        lastSampleTimeNanos = newSampleTimeNanos;

        return threads;
    }

    public void pause(long mills) {
        try {
            Thread.sleep(mills);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isIncludeInternalThreads() {
        return includeInternalThreads;
    }

    public void setIncludeInternalThreads(boolean includeInternalThreads) {
        this.includeInternalThreads = includeInternalThreads;
    }

}
