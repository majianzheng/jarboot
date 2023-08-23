package io.github.majianzheng.jarboot.core.constant;

/**
 * jarboot-service向客户端发送的指令定义
 * @author majianzheng
 */
public class CoreConstant {
    public static final String START_DETERMINE_TIME_KEY = "start.determine.time";
    /** ig: -Djarboot.stdout.file=start.out */
    public static final String STD_OUT_FILE = "jarboot.stdout.file";
    /** jarboot.stdout.file.always=true则一直记录，否则进程启动成功后停止记录 */
    public static final String STD_OUT_FILE_ALWAYS = "jarboot.stdout.file.always";
    
    public static final String DUMP_DIR = "classdump";

    /**
     * 使用websocket发送的最大字节阈值
     * 向jarboot-service发送的长度小于该值时使用WebSocket发送，否则通过http
     * 为了增大jarboot-service的最大连接数，WebSocket的服务端将接收缓存配置的较小
     */
    public static final int SOCKET_MAX_SEND = 4000;

    /**
     * 方法执行耗时
     */
    public static final String COST_VARIABLE = "cost";

    public static final String EXPRESS_DESCRIPTION = "  The express may be one of the following expression (evaluated dynamically):\n" +
            "          target : the object\n" +
            "           clazz : the object's class\n" +
            "          method : the constructor or method\n" +
            "          params : the parameters array of method\n" +
            "    params[0..n] : the element of parameters array\n" +
            "       returnObj : the returned object of method\n" +
            "        throwExp : the throw exception of method\n" +
            "        isReturn : the method ended by return\n" +
            "         isThrow : the method ended by throwing exception\n" +
            "           #cost : the execution time in ms of method invocation";

    public static final String EXAMPLE = "\nEXAMPLES:\n";

    public static final String WIKI = "\nWIKI:\n";

    public static final String WIKI_HOME = "  https://www.yuque.com/jarboot/usage/command#";

    public static final String EXPRESS_EXAMPLES =   "Examples:\n" +
            "  params\n" +
            "  params[0]\n" +
            "  'params[0]+params[1]'\n" +
            "  '{params[0], target, returnObj}'\n" +
            "  returnObj\n" +
            "  throwExp\n" +
            "  target\n" +
            "  clazz\n" +
            "  method\n";

    public static final String CONDITION_EXPRESS =  "Conditional expression in ognl style, for example:\n" +
            "  TRUE  : 1==1\n" +
            "  TRUE  : true\n" +
            "  FALSE : false\n" +
            "  TRUE  : 'params.length>=0'\n" +
            "  FALSE : 1==2\n";
    private CoreConstant(){}
}
