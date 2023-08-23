package io.github.majianzheng.jarboot.core.cmd.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author majianzheng
 */
public class JvmModel extends ResultModel {
    private List<JvmItem> runtimeInfo = new ArrayList<>();
    private List<JvmItem> classLoadingInfo = new ArrayList<>();
    private List<JvmItem> compilation = new ArrayList<>();
    private List<JvmItem> memoryInfo = new ArrayList<>();
    private List<JvmItem> operatingSystemInfo = new ArrayList<>();
    private List<GarbageCollectorItem> garbageCollectorsInfo = new ArrayList<>();
    private long pendingFinalizationCount = -1;
    private List<JvmItem> memoryMgrInfo = new ArrayList<>();
    private List<JvmItem> fileDescInfo = new ArrayList<>();

    private List<JvmItem> threadInfo = new ArrayList<>();

    @Override
    public String getName() {
        return "jvm";
    }

    public static class GarbageCollectorItem {
        private String name;
        private long collectionCount;
        private long collectionTime;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getCollectionCount() {
            return collectionCount;
        }

        public void setCollectionCount(long collectionCount) {
            this.collectionCount = collectionCount;
        }

        public long getCollectionTime() {
            return collectionTime;
        }

        public void setCollectionTime(long collectionTime) {
            this.collectionTime = collectionTime;
        }
    }

    public JvmModel appendRuntimeInfo(String name, Object value) {
        JvmItem item = new JvmItem();
        item.setName(name);
        item.setValue(value);
        runtimeInfo.add(item);
        return this;
    }

    public JvmModel appendClassLoadingInfo(String name, Object value) {
        JvmItem item = new JvmItem();
        item.setName(name);
        item.setValue(value);
        classLoadingInfo.add(item);
        return this;
    }

    public void appendCompilationInfo(String name, Object value) {
        JvmItem item = new JvmItem();
        item.setName(name);
        item.setValue(value);
        compilation.add(item);
    }

    public void appendMemoryMgrInfo(String name, String[] value) {
        JvmItem item = new JvmItem();
        item.setName(name);
        item.setValue(Arrays.toString(value));
        memoryMgrInfo.add(item);
    }

    public void appendFileDescInfo(String name, Object value) {
        JvmItem item = new JvmItem();
        item.setName(name);
        item.setValue(value);
        fileDescInfo.add(item);
    }

    public void appendGarbageCollectorItem(String name, long collectionCount, long collectionTime) {
        GarbageCollectorItem item = new GarbageCollectorItem();
        item.setName(name);
        item.setCollectionCount(collectionCount);
        item.setCollectionTime(collectionTime);
        garbageCollectorsInfo.add(item);
    }

    public JvmModel appendMemoryInfo(String name, Object value) {
        JvmItem item = new JvmItem();
        item.setName(name);
        item.setValue(value);
        memoryInfo.add(item);
        return this;
    }

    public JvmModel appendSystemInfo(String name, Object value) {
        JvmItem item = new JvmItem();
        item.setName(name);
        item.setValue(value);
        operatingSystemInfo.add(item);
        return this;
    }

    public JvmModel appendThreadInfo(String name, Object value) {
        JvmItem item = new JvmItem();
        item.setName(name);
        item.setValue(value);
        threadInfo.add(item);
        return this;
    }

    public List<JvmItem> getRuntimeInfo() {
        return runtimeInfo;
    }

    public void setRuntimeInfo(List<JvmItem> runtimeInfo) {
        this.runtimeInfo = runtimeInfo;
    }

    public List<JvmItem> getMemoryInfo() {
        return memoryInfo;
    }

    public void setMemoryInfo(List<JvmItem> memoryInfo) {
        this.memoryInfo = memoryInfo;
    }

    public List<JvmItem> getOperatingSystemInfo() {
        return operatingSystemInfo;
    }

    public void setOperatingSystemInfo(List<JvmItem> operatingSystemInfo) {
        this.operatingSystemInfo = operatingSystemInfo;
    }

    public List<JvmItem> getClassLoadingInfo() {
        return classLoadingInfo;
    }

    public void setClassLoadingInfo(List<JvmItem> classLoadingInfo) {
        this.classLoadingInfo = classLoadingInfo;
    }

    public List<JvmItem> getCompilation() {
        return compilation;
    }

    public void setCompilation(List<JvmItem> compilation) {
        this.compilation = compilation;
    }

    public List<JvmItem> getThreadInfo() {
        return threadInfo;
    }

    public void setThreadInfo(List<JvmItem> threadInfo) {
        this.threadInfo = threadInfo;
    }

    public List<GarbageCollectorItem> getGarbageCollectorsInfo() {
        return garbageCollectorsInfo;
    }

    public void setGarbageCollectorsInfo(List<GarbageCollectorItem> garbageCollectorsInfo) {
        this.garbageCollectorsInfo = garbageCollectorsInfo;
    }

    public long getPendingFinalizationCount() {
        return pendingFinalizationCount;
    }

    public void setPendingFinalizationCount(long pendingFinalizationCount) {
        this.pendingFinalizationCount = pendingFinalizationCount;
    }

    public List<JvmItem> getMemoryMgrInfo() {
        return memoryMgrInfo;
    }

    public void setMemoryMgrInfo(List<JvmItem> memoryMgrInfo) {
        this.memoryMgrInfo = memoryMgrInfo;
    }

    public List<JvmItem> getFileDescInfo() {
        return fileDescInfo;
    }

    public void setFileDescInfo(List<JvmItem> fileDescInfo) {
        this.fileDescInfo = fileDescInfo;
    }
}
