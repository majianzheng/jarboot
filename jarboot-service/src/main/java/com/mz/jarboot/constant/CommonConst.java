package com.mz.jarboot.constant;


public class CommonConst {
    public static final String WORKSPACE_HOME= "workspace.home"; //用户工作目录
    public static final String JARBOOT_HOME= "jarboot.home"; //当前执行目录

    //协议分隔符
    public static final char PROTOCOL_SPLIT = '\r';

    //运行状态
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_STOPPED = "STOPPED";
    public static final String STATUS_STARTING = "STARTING";
    public static final String STATUS_STOPPING = "STOPPING";

    public static final int NOTICE_INFO = 0;
    public static final int NOTICE_WARN = 1;
    public static final int NOTICE_ERROR = 2;

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
