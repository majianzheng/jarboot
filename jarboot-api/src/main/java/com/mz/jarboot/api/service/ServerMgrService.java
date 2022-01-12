package com.mz.jarboot.api.service;

import com.mz.jarboot.api.pojo.JvmProcess;
import com.mz.jarboot.api.pojo.ServerRunning;
import com.mz.jarboot.api.pojo.ServerSetting;

import java.util.List;

/**
 * 服务管理
 * @author majianzheng
 */
public interface ServerMgrService {

    /**
     * 获取服务列表
     * @return 服务列表
     */
    List<ServerRunning> getServerList();

    /**
     * 获取服务信息
     * @param name 服务名称
     * @return 服务信息 {@link ServerRunning}
     */
    ServerRunning getServer(String name);

    /**
     * 一键重启，杀死所有服务进程，根据依赖重启
     */
    void oneClickRestart();

    /**
     * 一键启动，根据依赖重启
     */
    void oneClickStart();

    /**
     * 一键停止，杀死所有服务进程
     */
    void oneClickStop();

    /**
     * 启动服务
     * @param paths 服务列表，字符串格式：服务path
     */
    void startServer(List<String> paths);

    /**
     * 停止服务
     * @param paths 服务列表，字符串格式：服务path
     */
    void stopServer(List<String> paths);

    /**
     * 重启服务
     * @param paths 服务列表，字符串格式：服务path
     */
    void restartServer(List<String> paths);

    /**
     * 通过服务配置启动服务
     * @param setting 服务配置
     */
    void startSingleServer(ServerSetting setting);

    /**
     * 获取未被服务管理的JVM进程信息
     * @return jvm进程信息
     */
    List<JvmProcess> getJvmProcesses();

    /**
     * attach到指定的进程
     * @param pid 进程pid
     */
    void attach(String pid);

    /**
     * 删除服务
     * @param server 服务名
     */
    void deleteServer(String server);
}
