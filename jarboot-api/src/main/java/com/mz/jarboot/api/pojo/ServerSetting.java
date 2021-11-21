package com.mz.jarboot.api.pojo;

import com.mz.jarboot.api.constant.SettingPropConst;
import java.io.Serializable;

/**
 * 服务的运行配置
 * @author majianzheng
 */
public class ServerSetting implements Serializable {
    /**
     * 服务名，即jar文件的上级目录的名称
     */
    private String name;

    /**
     * 组名字，可为空
     */
    private String group;

    /**
     * sid，服务唯一标识，通过path计算得出
     */
    private transient String sid;

    /**
     * 最后修改时间
     */
    private transient long lastModified;

    /**
     * 服务的文件夹路径
     */
    private String path;
    
    /**
     * 用户自定义的启动命令
     * 若为空，且目录有唯一一个jar文件时，使用-jar选项启动
     */
    private String command;
    
    /**
     * 自定义的JVM参数
     */
    private String vm;
    
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
    private String workDirectory;
    
    /**
     * 指定使用的Jdk，默认继承父进程
     */
    private String jdkPath;
    
    /**
     * 环境变量
     */
    private String env;
    
    /**
     * 是否启用守护，启用后，若服务异常退出则自动启动
     */
    private Boolean daemon;
    
    /**
     * jar文件改动监控，启用后，若jar文件更新则自动重启，已经处于关闭状态的不会启动
     */
    private Boolean jarUpdateWatch;

    public ServerSetting() {
        //默认设定
        this(SettingPropConst.DEFAULT_VM_FILE, 1, "", true, true);
    }

    public ServerSetting(String name) {
        //默认设定
        this(SettingPropConst.DEFAULT_VM_FILE, 1, "", true, true);
        this.name = name;
    }

    private ServerSetting(String vm, Integer priority, String args, Boolean daemon, Boolean jarUpdateWatch) {
        this.vm = vm;
        this.priority = priority;
        this.args = args;
        this.daemon = daemon;
        this.jarUpdateWatch = jarUpdateWatch;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
    
    public String getCommand() {
        return command;
    }
    
    public void setCommand(String command) {
        this.command = command;
    }

    public String getVm() {
        return vm;
    }

    public void setVm(String vm) {
        this.vm = vm;
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

    public String getWorkDirectory() {
        return workDirectory;
    }

    public void setWorkDirectory(String workDirectory) {
        this.workDirectory = workDirectory;
    }

    public String getJdkPath() {
        return jdkPath;
    }

    public void setJdkPath(String jdkPath) {
        this.jdkPath = jdkPath;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
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

    @Override
    public String toString() {
        return "ServerSettingDTO{" +
                "server='" + name + '\'' +
                ", command='" + command + '\'' +
                ", vm='" + vm + '\'' +
                ", priority=" + priority +
                ", args='" + args + '\'' +
                ", workDirectory='" + workDirectory + '\'' +
                ", jdkPath='" + jdkPath + '\'' +
                ", env='" + env + '\'' +
                ", daemon=" + daemon +
                ", jarUpdateWatch=" + jarUpdateWatch +
                '}';
    }
}
