package com.mz.jarboot.constant;


public class CommonConst {
    public static final String WORKSPACE_HOME= "workspace.home"; //用户工作目录
    public static final String JARBOOT_HOME= "JARBOOT_HOME"; //当前执行目录

    //服务目录名称
    public static final String SERVICES_DIR = "services";

    //运行状态
    public static final String STATUS_RUNNING = "正在运行";
    public static final String STATUS_STOPPED = "已停止";
    public static final String STATUS_STARTING = "正在启动";
    public static final String STATUS_STOPPING = "正在停止";

    public static final String NOTICE_INFO = "NOTICE_INFO";
    public static final String NOTICE_WARN = "NOTICE_WARN";
    public static final String NOTICE_ERROR = "NOTICE_ERROR";

    public static final int INVALID_PID = -1;

    //等待目标进程退出的最大时间，毫秒
    public static final int MAX_WAIT_EXIT_TIME = 5000;
    public static final int MAX_RESPONSE_TIME = 30; //秒

    public static final String ARG_KILL_ALL = "-killAllServer";
    public static final String ARG_START_ALL = "-startAllServer";
    public static final String ARG_IGNORE_LOCK = "-ignoreLock";

    public static final String AGENT_JAR_NAME = "jarboot-agent.jar";

    private CommonConst(){}
}
