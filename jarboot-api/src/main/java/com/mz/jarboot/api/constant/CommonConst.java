package com.mz.jarboot.api.constant;


/**
 * @author majianzheng
 */
@SuppressWarnings("all")
public class CommonConst {
    public static final String JARBOOT_HOME= "JARBOOT_HOME";
    public static final String REMOTE_PROP = "jarboot.remote";
    public static final String SERVER_NAME_PROP = "jarboot.name";
    public static final String JARBOOT_HOST_ENV = "JARBOOT_HOST";

    public static final String PORT_KEY = "server.port";
    public static final String DEFAULT_PORT = "9899";

    public static final String BIN_NAME = "bin";
    public static final String JAVA_CMD = "java";
    public static final String EXE_EXT = ".exe";
    public static final String JAR_EXT = ".jar";
    public static final String ARG_JAR = "-jar ";
    
    public static final String DOT = ".";
    public static final char EQUAL_CHAR = '=';
    
    public static final String COMMA_SPLIT = ",";
    public static final String[] JAR_FILE_EXT = new String[]{"jar"};

    public static final String JARBOOT_NAME = "jarboot";
    public static final String SERVICES = "services";

    public static final String HTTP = "http://";
    public static final String HTTPS = "https://";
    public static final String WS = "ws://";

    /** Controller context */
    public static final String SERVER_MGR_CONTEXT = "/api/jarboot/services";
    public static final String AUTH_CONTEXT = "/api/jarboot/auth";
    public static final String AGENT_CLIENT_CONTEXT = "/api/jarboot/public/agent";
    public static final String CLOUD_CONTEXT = "/api/jarboot/cloud";
    public static final String PLUGINS_CONTEXT = "/api/jarboot/plugins";
    public static final String PRIVILEGE_CONTEXT = "/api/jarboot/privilege";
    public static final String ROLE_CONTEXT = "/api/jarboot/role";
    public static final String SETTING_CONTEXT = "/api/jarboot/setting";
    public static final String USER_CONTEXT = "/api/jarboot/user";
    public static final String AGENT_WS_CONTEXT = "/jarboot/public/agent/ws";

    public static final String SERVER_PARAM = "server";
    public static final String SID_PARAM = "sid";

    /**
     * 等待目标进程优雅退出的最大时间，毫秒
     */
    public static final int MAX_WAIT_EXIT_TIME = 30000;

    /**
     * Agent最大连接超时时间（秒）
     */
    public static final int MAX_AGENT_CONNECT_TIME = 15;

    public static final String AGENT_JAR_NAME = "jarboot-agent.jar";

    public static final String REMOTE_SID_PREFIX = "remote-";

    private CommonConst(){}
}
