package io.github.majianzheng.jarboot.api.pojo;

import io.github.majianzheng.jarboot.api.constant.SettingPropConst;
import java.io.Serializable;
import java.util.Objects;

/**
 * 服务的运行配置
 * @author majianzheng
 */
public class ServiceSetting implements Serializable {
    /** 所属集群实例 */
    private String host;
    /**
     * 服务名，即jar文件的上级目录的名称
     */
    private String name;

    /**
     * 用户目录
     */
    private String userDir;

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
    private transient Long lastModified;
    
    /**
     * 用户自定义的启动命令
     * 若为空，且目录有唯一一个jar文件时，使用-jar选项启动
     */
    private String command;
    
    /**
     * 自定义的JVM参数文件
     */
    private String vm;

    /**
     * 自定义的JVM参数文件内容
     */
    private String vmContent;
    
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
     * 文件改动监控，启用后，若文件更新则自动重启，已经处于关闭状态的不会启动
     */
    private Boolean fileUpdateWatch;

    /** 应用类型 java or shell */
    private String applicationType;

    private String scheduleType;

    /** 周期执行计划，cron表达式 */
    private String cron;

    public ServiceSetting() {
        //默认设定
        this(SettingPropConst.DEFAULT_VM_FILE, 1, "", true, true);
    }

    public ServiceSetting(String name) {
        //默认设定
        this(SettingPropConst.DEFAULT_VM_FILE, 1, "", true, true);
        this.name = name;
    }

    private ServiceSetting(String vm, Integer priority, String args, Boolean daemon, Boolean fileUpdateWatch) {
        this.vm = vm;
        this.priority = priority;
        this.args = args;
        this.daemon = daemon;
        this.fileUpdateWatch = fileUpdateWatch;
        this.applicationType = "java";
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

    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
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

    public String getVmContent() {
        return vmContent;
    }

    public void setVmContent(String vmContent) {
        this.vmContent = vmContent;
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

    public Boolean getFileUpdateWatch() {
        return fileUpdateWatch;
    }

    public void setFileUpdateWatch(Boolean fileUpdateWatch) {
        this.fileUpdateWatch = fileUpdateWatch;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    public String getUserDir() {
        return userDir;
    }

    public void setUserDir(String userDir) {
        this.userDir = userDir;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public String toString() {
        return "ServiceSetting{" +
                "name='" + name + '\'' +
                ", group='" + group + '\'' +
                ", sid='" + sid + '\'' +
                ", lastModified=" + lastModified +
                ", command='" + command + '\'' +
                ", vm='" + vm + '\'' +
                ", priority=" + priority +
                ", args='" + args + '\'' +
                ", workDirectory='" + workDirectory + '\'' +
                ", jdkPath='" + jdkPath + '\'' +
                ", env='" + env + '\'' +
                ", daemon=" + daemon +
                ", fileUpdateWatch=" + fileUpdateWatch +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceSetting setting = (ServiceSetting) o;
        return name.equals(setting.name) && Objects.equals(userDir, setting.userDir) && Objects.equals(group, setting.group) && sid.equals(setting.sid) && Objects.equals(command, setting.command) && Objects.equals(vm, setting.vm) && Objects.equals(vmContent, setting.vmContent) && Objects.equals(priority, setting.priority) && Objects.equals(args, setting.args) && Objects.equals(workDirectory, setting.workDirectory) && Objects.equals(jdkPath, setting.jdkPath) && Objects.equals(env, setting.env) && Objects.equals(daemon, setting.daemon) && Objects.equals(fileUpdateWatch, setting.fileUpdateWatch) && Objects.equals(applicationType, setting.applicationType) && Objects.equals(scheduleType, setting.scheduleType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, userDir, sid);
    }
}
