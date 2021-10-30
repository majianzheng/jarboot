package com.mz.jarboot.api.constant;


/**
 * @author majianzheng
 */
@SuppressWarnings("all")
public class CommonConst {
    public static final String WORKSPACE_HOME= "workspace.home";
    public static final String JARBOOT_HOME= "jarboot.home";

    public static final String PORT_KEY = "server.port";
    public static final String DEFAULT_PORT = "9899";

    public static final String BIN_NAME = "bin";
    public static final String JAVA_CMD = "java";
    public static final String EXE_EXT = ".exe";
    public static final String JAR_EXT = ".jar";
    public static final String ARG_JAR = "-jar ";
    public static final String PID_EXT = ".pid";
    
    public static final String DOT = ".";
    public static final char EQUAL_CHAR = '=';
    
    public static final String COMMA_SPLIT = ",";
    public static final String[] JAR_FILE_EXT = new String[]{"jar"};

    public static final String JARBOOT_NAME = "jarboot";
    public static final String SERVICES = "services";

    /**
     * 运行状态
     */
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_STOPPED = "STOPPED";
    public static final String STATUS_STARTING = "STARTING";
    public static final String STATUS_STOPPING = "STOPPING";


    public static final int INVALID_PID = -1;

    /**
     * 等待目标进程优雅退出的最大时间，毫秒
     */
    public static final int MAX_WAIT_EXIT_TIME = 30000;

    /**
     * Agent最大连接超时时间（秒）
     */
    public static final int MAX_AGENT_CONNECT_TIME = 15;

    public static final String AGENT_JAR_NAME = "jarboot-agent.jar";

    private CommonConst(){}
}
