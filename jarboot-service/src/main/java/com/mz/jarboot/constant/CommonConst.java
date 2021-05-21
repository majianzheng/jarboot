package com.mz.jarboot.constant;


public class CommonConst {
    public static final String WORKSPACE_HOME= "workspace.home";

    public static final String PROP_FILE_EXT = "properties";
    public static final String ROOT_PATH_KEY = "root-path";
    public static final String DEBUG_MODE_KEY = "debug-mode";
    public static final String ROOT_DIR_KEY = "root-dir-name";

    //Web服务目录名称
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

    public static final String ARG_KILL_ALL = "-killAllServer";
    public static final String ARG_START_ALL = "-startAllServer";
    public static final String ARG_IGNORE_LOCK = "-ignoreLock";

    private CommonConst(){}
}
