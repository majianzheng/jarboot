package com.mz.jarboot.api.service;

import com.mz.jarboot.api.pojo.JvmProcess;

import java.util.List;

/**
 * 在线调试未受服务管理的Java进程
 * @author majianzheng
 */
public interface OnlineDebugService {
    /**
     * 获取未被服务管理的JVM进程信息
     * @return jvm进程信息
     */
    List<JvmProcess> getJvmProcesses();

    /**
     * attach到指定的进程
     * @param pid 进程pid
     * @param name 名字
     */
    void attach(int pid, String name);
}
