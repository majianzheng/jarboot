package io.github.majianzheng.jarboot.core.cmd.impl;

import io.github.majianzheng.jarboot.api.cmd.annotation.Description;
import io.github.majianzheng.jarboot.api.cmd.annotation.Name;
import io.github.majianzheng.jarboot.api.cmd.annotation.Summary;
import io.github.majianzheng.jarboot.core.cmd.AbstractCommand;
import io.github.majianzheng.jarboot.core.cmd.model.JvmModel;
import io.github.majianzheng.jarboot.core.constant.CoreConstant;

import java.lang.management.*;
import java.lang.reflect.Method;
import java.util.*;

/**
 * show the jvm detail
 * @author majianzheng
 */
@Name("jvm")
@Summary("Display the target JVM information")
@Description(CoreConstant.WIKI + CoreConstant.WIKI_HOME + "jvm")
@SuppressWarnings({"java:S1874"})
public class JvmCommand extends AbstractCommand {
    private final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private final ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
    private final CompilationMXBean compilationMXBean = ManagementFactory.getCompilationMXBean();
    private final Collection<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
    private final Collection<MemoryManagerMXBean> memoryManagerMXBeans = ManagementFactory.getMemoryManagerMXBeans();
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private JvmModel model = new JvmModel();

    private void appendRuntime() {
        model.appendRuntimeInfo("MACHINE-NAME", runtimeMXBean.getName())
                .appendRuntimeInfo("JVM-START-TIME", runtimeMXBean.getStartTime())
                .appendRuntimeInfo("MANAGEMENT-SPEC-VERSION", runtimeMXBean.getManagementSpecVersion())
                .appendRuntimeInfo("SPEC-NAME", runtimeMXBean.getSpecName())
                .appendRuntimeInfo("SPEC-VENDOR", runtimeMXBean.getSpecVersion())
                .appendRuntimeInfo("VM-NAME", runtimeMXBean.getVmName())
                .appendRuntimeInfo("VM-VENDOR", runtimeMXBean.getVmVendor())
                .appendRuntimeInfo("VM-VERSION", runtimeMXBean.getVmVersion())
                .appendRuntimeInfo("INPUT-ARGUMENTS", runtimeMXBean.getInputArguments())
                .appendRuntimeInfo("CLASS-PATH", runtimeMXBean.getClassPath())
                //.appendRuntimeInfo("BOOT-CLASS-PATH", runtimeMXBean.getBootClassPath())
                .appendRuntimeInfo("LIBRARY-PATH", runtimeMXBean.getLibraryPath())
                ;
    }

    private void appendClassLoading() {
        model.appendClassLoadingInfo("LOADED-CLASS-COUNT", classLoadingMXBean.getLoadedClassCount())
        .appendClassLoadingInfo("TOTAL-LOADED-CLASS-COUNT", classLoadingMXBean.getTotalLoadedClassCount())
        .appendClassLoadingInfo("UNLOADED-CLASS-COUNT", classLoadingMXBean.getUnloadedClassCount())
        .appendClassLoadingInfo("IS-VERBOSE", classLoadingMXBean.isVerbose());
    }

    private void addCompilation() {
        if (compilationMXBean == null) {
            return;
        }
        model.appendCompilationInfo("NAME", compilationMXBean.getName());
        if (compilationMXBean.isCompilationTimeMonitoringSupported()) {
            model.appendCompilationInfo("TOTAL-COMPILE-TIME", compilationMXBean.getTotalCompilationTime() + "(ms)");
        }
    }

    private void appendSystemDetail() {
        OperatingSystemMXBean osb = ManagementFactory.getOperatingSystemMXBean();
        model.appendSystemInfo("OS", osb.getName())
                .appendSystemInfo("ARCH", osb.getArch())
                .appendSystemInfo("PROCESSORS-COUNT", osb.getAvailableProcessors())
                .appendSystemInfo("VERSION", osb.getVersion())
                .appendSystemInfo("LOAD-AVERAGE", osb.getSystemLoadAverage())
                ;
    }

    private void addGarbageCollectors() {
        for (GarbageCollectorMXBean gcMXBean : garbageCollectorMXBeans) {
            model.appendGarbageCollectorItem(gcMXBean.getName(), gcMXBean.getCollectionCount(), gcMXBean.getCollectionTime());
        }
    }

    private void addMemoryManagers() {
        for (final MemoryManagerMXBean memoryManagerMXBean : memoryManagerMXBeans) {
            if (memoryManagerMXBean.isValid()) {
                final String name = memoryManagerMXBean.isValid()
                        ? memoryManagerMXBean.getName()
                        : memoryManagerMXBean.getName() + "(Invalid)";
                model.appendMemoryMgrInfo(name, memoryManagerMXBean.getMemoryPoolNames());
            }
        }
    }

    private void appendMemory() {
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();

        model.appendMemoryInfo("HEAP-MEMORY-USAGE", heapMemoryUsage)
                .appendMemoryInfo("NO-HEAP-MEMORY-USAGE", nonHeapMemoryUsage);
        int pendingFinalizationCount = memoryMXBean.getObjectPendingFinalizationCount();
        model.setPendingFinalizationCount(pendingFinalizationCount);
    }

    private void appendThread() {
        model.appendThreadInfo("COUNT", threadMXBean.getThreadCount())
                .appendThreadInfo("DAEMON-COUNT", threadMXBean.getDaemonThreadCount())
                .appendThreadInfo("PEAK-COUNT", threadMXBean.getPeakThreadCount())
                .appendThreadInfo("STARTED-COUNT", threadMXBean.getTotalStartedThreadCount())
                .appendThreadInfo("DEADLOCK-COUNT", getDeadlockedThreadsCount(threadMXBean));
    }
    private int getDeadlockedThreadsCount(ThreadMXBean threads) {
        final long[] ids = threads.findDeadlockedThreads();
        if (ids == null) {
            return 0;
        } else {
            return ids.length;
        }
    }

    private void addFileDescriptor() {
        model.appendFileDescInfo("MAX-FILE-DESCRIPTOR-COUNT", invokeFileDescriptor(operatingSystemMXBean, "getMaxFileDescriptorCount"));
        model.appendFileDescInfo("OPEN-FILE-DESCRIPTOR-COUNT", invokeFileDescriptor(operatingSystemMXBean, "getOpenFileDescriptorCount"));
    }

    @SuppressWarnings("java:S3011")
    private long invokeFileDescriptor(OperatingSystemMXBean os, String name) {
        try {
            final Method method = os.getClass().getDeclaredMethod(name);
            method.setAccessible(true);
            return (Long) method.invoke(os);
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void run() {
        appendRuntime();
        appendClassLoading();
        addCompilation();
        addGarbageCollectors();
        appendSystemDetail();
        appendMemory();
        appendThread();
        addMemoryManagers();
        addFileDescriptor();

        session.appendResult(model);

        //一次性类型命令直接结束
        session.end();
    }
}
