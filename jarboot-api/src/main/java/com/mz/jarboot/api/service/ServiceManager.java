package com.mz.jarboot.api.service;

import com.mz.jarboot.api.constant.TaskLifecycle;
import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.api.event.TaskLifecycleEvent;
import com.mz.jarboot.api.pojo.JvmProcess;
import com.mz.jarboot.api.pojo.ServiceInstance;
import com.mz.jarboot.api.pojo.ServiceSetting;

import java.util.List;

/**
 * 服务管理
 * @author majianzheng
 */
public interface ServiceManager {

    /**
     * 获取服务列表
     * @return 服务列表
     */
    List<ServiceInstance> getServiceList();

    /**
     * 获取服务信息
     * @param serviceName 服务名称
     * @return 服务信息 {@link ServiceInstance}
     */
    ServiceInstance getService(String serviceName);

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
     * @param serviceNames 服务列表
     */
    void startService(List<String> serviceNames);

    /**
     * 停止服务
     * @param serviceNames 服务列表，字符串格式：服务path
     */
    void stopService(List<String> serviceNames);

    /**
     * 重启服务
     * @param serviceNames 服务列表，字符串格式：服务path
     */
    void restartService(List<String> serviceNames);

    /**
     * 通过服务配置启动服务
     * @param setting 服务配置
     */
    void startSingleService(ServiceSetting setting);

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
     * @param serviceName 服务名
     */
    void deleteService(String serviceName);

    /**
     * 注册事件处理
     * @param serviceName 服务名称
     * @param lifecycle 任务生命周期 {@link TaskLifecycle}
     * @param subscriber 任务处理 {@link Subscriber}
     */
    void registerSubscriber(String serviceName,
                            TaskLifecycle lifecycle,
                            Subscriber<TaskLifecycleEvent> subscriber);

    /**
     * 反注册事件处理
     * @param serviceName 服务名称
     * @param lifecycle 任务生命周期 {@link TaskLifecycle}
     * @param subscriber 任务处理 {@link Subscriber}
     */
    void deregisterSubscriber(String serviceName,
                              TaskLifecycle lifecycle,
                              Subscriber<TaskLifecycleEvent> subscriber);
}
