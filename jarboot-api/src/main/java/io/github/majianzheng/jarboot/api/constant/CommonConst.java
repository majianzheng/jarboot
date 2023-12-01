package io.github.majianzheng.jarboot.api.constant;


/**
 * @author majianzheng
 */
public class CommonConst {
    public static final String JARBOOT_HOME= "JARBOOT_HOME";
    public static final String REMOTE_PROP = "jarboot.remote";
    public static final String SERVER_NAME_PROP = "jarboot.name";
    public static final String JARBOOT_HOST_ENV = "JARBOOT_HOST";

    public static final String PORT_KEY = "server.port";
    public static final int DEFAULT_PORT = 9899;

    public static final String BIN_NAME = "bin";
    public static final String COMPONENTS_NAME = "components";
    public static final String JAVA_CMD = "java";
    public static final String EXE_EXT = ".exe";
    public static final String JAR_EXT = ".jar";
    public static final String ARG_JAR = "-jar ";
    
    public static final String DOT = ".";
    public static final char EQUAL_CHAR = '=';
    
    public static final String COMMA_SPLIT = ",";
    public static final String JAR_FILE_EXT = "jar";

    public static final String JARBOOT_NAME = "jarboot";
    public static final String WORKSPACE = "workspace";

    public static final String HTTP = "http://";
    public static final String HTTPS = "https://";
    public static final String WS = "ws://";
    /** 心跳ping */
    public static final String PING = "ping";

    /** Controller context */
    public static final String SERVICE_MGR_CONTEXT = "/api/jarboot/services";
    public static final String AUTH_CONTEXT = "/api/jarboot/auth";
    public static final String AGENT_CLIENT_CONTEXT = "/api/jarboot/public/agent";
    public static final String SERVER_RUNTIME_CONTEXT = "/api/jarboot/public/serverRuntime";
    public static final String PLUGINS_CONTEXT = "/api/jarboot/plugins";
    public static final String PRIVILEGE_CONTEXT = "/api/jarboot/privilege";
    public static final String ROLE_CONTEXT = "/api/jarboot/role";
    public static final String SETTING_CONTEXT = "/api/jarboot/setting";
    public static final String USER_CONTEXT = "/api/jarboot/user";
    public static final String CLUSTER_CONTEXT = "/api/jarboot/cluster";
    public static final String CLUSTER_API_CONTEXT = CLUSTER_CONTEXT + "/api";
    public static final String CLUSTER_WS_CONTEXT = CLUSTER_CONTEXT + "/ws";
    public static final String CLUSTER_MGR_CONTEXT = CLUSTER_CONTEXT + "/manager";
    public static final String AGENT_WS_CONTEXT = "/jarboot/public/agent/ws";
    public static final String EVENT_WS_CONTEXT = "/jarboot/event/ws";
    public static final String MAIN_WS_CONTEXT = "/jarboot/main/service/ws";

    public static final String SERVICE_NAME_PARAM = "serviceName";
    public static final String SID_PARAM = "sid";

    public static final String USER_DIR = "userDir";

    /** Task status */
    public static final String STARTING = "STARTING";
    public static final String RUNNING = "RUNNING";
    public static final String STOPPING = "STOPPING";
    public static final String STOPPED = "STOPPED";
    public static final String SCHEDULING = "SCHEDULING";
    public static final String ATTACHED = "ATTACHED";
    public static final String NOT_ATTACHED = "NOT_ATTACHED";
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

    public static final String SHELL_TYPE = "shell";

    public static final int NODE_ROOT = 1;
    public static final int NODE_GROUP = 2;

    public static final String POST_EXCEPTION_TASK_SUFFIX = "后置脚本启动";

    public static final String DOCKER = "docker";

    private CommonConst(){}
}
