package com.mz.jarboot.dto;

import java.io.Serializable;

/**
 * 服务的运行配置
 * @author majianzheng
 */
public class ServerSettingDTO implements Serializable {
    /**
     * 服务名，即jar文件的上级目录的名称
     */
    private transient String server;
    /**
     * 启动的主类MainClass所在的jar文件
     * 若为空，则空目录下找第一个jar文件
     */
    private String jar;
    /**
     * 自定义的JVM参数
     */
    private String jvm;
    /**
     * 启动的优先级，从1开始，越大优先级越高，最高的优先启动
     * 未配置或小于1时，默认为1
     */
    private Integer priority;
    /**
     * 传入main函数的启动参数
     */
    private String args;
    /**
     * Java进程的工作目录
     */
    private String workHome;
    /**
     * 环境变量
     */
    private String envp = "";
    /**
     * 是否启用守护，启用后，若服务异常退出则自动启动
     */
    private Boolean daemon;
    /**
     * jar文件改动监控，启用后，若jar文件更新则自动重启，已经处于关闭状态的不会启动
     */
    private Boolean jarUpdateWatch;

    public ServerSettingDTO() {
        //默认设定
        this("", 1, "", true, true);
    }

    public ServerSettingDTO(String server) {
        //默认设定
        this("", 1, "", true, true);
        this.server = server;
    }

    private ServerSettingDTO(String jvm, Integer priority, String args, Boolean daemon, Boolean jarUpdateWatch) {
        this.jvm = jvm;
        this.priority = priority;
        this.args = args;
        this.daemon = daemon;
        this.jarUpdateWatch = jarUpdateWatch;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getJar() {
        return jar;
    }

    public void setJar(String jar) {
        this.jar = jar;
    }

    public String getJvm() {
        return jvm;
    }

    public void setJvm(String jvm) {
        this.jvm = jvm;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public String getWorkHome() {
        return workHome;
    }

    public void setWorkHome(String workHome) {
        this.workHome = workHome;
    }

    public String getEnvp() {
        return envp;
    }

    public void setEnvp(String envp) {
        this.envp = envp;
    }

    public Boolean getDaemon() {
        return daemon;
    }

    public void setDaemon(Boolean daemon) {
        this.daemon = daemon;
    }

    public Boolean getJarUpdateWatch() {
        return jarUpdateWatch;
    }

    public void setJarUpdateWatch(Boolean jarUpdateWatch) {
        this.jarUpdateWatch = jarUpdateWatch;
    }
}
