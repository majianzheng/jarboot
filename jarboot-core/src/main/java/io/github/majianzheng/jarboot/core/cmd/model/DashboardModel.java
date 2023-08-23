package io.github.majianzheng.jarboot.core.cmd.model;

import java.util.List;
import java.util.Map;

/**
 * Model of 'dashboard' command
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class DashboardModel extends ResultModel {
    private List<ThreadVO> threads;
    private Map<String, List<MemoryEntryVO>> memoryInfo;
    private List<GcInfoVO> gcInfos;
    private RuntimeInfoVO runtimeInfo;
    private TomcatInfoVO tomcatInfo;

    @Override
    public String getName() {
        return "dashboard";
    }

    public List<ThreadVO> getThreads() {
        return threads;
    }

    public void setThreads(List<ThreadVO> threads) {
        this.threads = threads;
    }

    public Map<String, List<MemoryEntryVO>> getMemoryInfo() {
        return memoryInfo;
    }

    public void setMemoryInfo(Map<String, List<MemoryEntryVO>> memoryInfo) {
        this.memoryInfo = memoryInfo;
    }

    public List<GcInfoVO> getGcInfos() {
        return gcInfos;
    }

    public void setGcInfos(List<GcInfoVO> gcInfos) {
        this.gcInfos = gcInfos;
    }

    public RuntimeInfoVO getRuntimeInfo() {
        return runtimeInfo;
    }

    public void setRuntimeInfo(RuntimeInfoVO runtimeInfo) {
        this.runtimeInfo = runtimeInfo;
    }

    public TomcatInfoVO getTomcatInfo() {
        return tomcatInfo;
    }

    public void setTomcatInfo(TomcatInfoVO tomcatInfo) {
        this.tomcatInfo = tomcatInfo;
    }
}
