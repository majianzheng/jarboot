package io.github.majianzheng.jarboot.core;


import com.alibaba.bytekit.utils.JavaVersionUtils;

/**
 * 全局开关
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
@SuppressWarnings({"squid:S1444", "squid:S1125", "squid:S1118"})
public class GlobalOptions {

    /**
     * 是否支持系统类<br/>
     * 这个开关打开之后将能代理到来自JVM的部分类，由于有非常强的安全风险可能会引起系统崩溃<br/>
     * 所以这个开关默认是关闭的，除非你非常了解你要做什么，否则请不要打开
     */
    @Option(level = 0,
            name = "unsafe",
            summary = "Option to support system-level class",
            description  =
                    "This option enables to proxy functionality of JVM classes."
                            +  " Due to serious security risk a JVM crash is possibly be introduced."
                            +  " Do not activate it unless you are able to manage."
    )
    public static volatile boolean isUnsafe = false;

    /**
     * 是否支持dump被增强的类<br/>
     * 这个开关打开这后，每次增强类的时候都将会将增强的类dump到文件中，以便于进行反编译分析
     */
    @Option(level = 1,
            name = "dump",
            summary = "Option to dump the enhanced classes",
            description =
                    "This option enables the enhanced classes to be dumped to external file " +
                            "for further de-compilation and analysis."
    )
    public static volatile boolean isDump = false;

    /**
     * 是否支持批量增强<br/>
     * 这个开关打开后，每次均是批量增强类
     */
    @Option(level = 1,
            name = "batch-re-transform",
            summary = "Option to support batch reTransform Class",
            description = "This options enables to reTransform classes with batch mode."
    )
    public static volatile boolean isBatchReTransform = true;

    /**
     * 是否支持json格式化输出<br/>
     * 这个开关打开后，使用json格式输出目标对象，配合-x参数使用
     */
    @Option(level = 2,
            name = "json-format",
            summary = "Option to support JSON format of object output",
            description = "This option enables to format object output with JSON when -x option selected."
    )
    public static volatile boolean isUsingJson = false;

    /**
     * 是否关闭子类
     */
    @Option(
            level = 1,
            name = "disable-sub-class",
            summary = "Option to control include sub class when class matching",
            description = "This option disable to include sub class when matching class."
    )
    public static volatile boolean isDisableSubClass = false;

    /**
     * 是否在interface类里搜索函数
     */
    @Option(
            level = 1,
            name = "support-default-method",
            summary = "Option to control include default method in interface when class matching",
            description = "This option disable to include default method in interface when matching class."
    )
    public static volatile boolean isSupportDefaultMethod = true && JavaVersionUtils.isGreaterThanJava7();

    /**
     * 是否日志中保存命令执行结果
     */
    @Option(level = 1,
            name = "save-result",
            summary = "Option to print command's result to log file",
            description = "This option enables to save each command's result to log file, " +
                    "which path is ${user.home}/logs/arthas-cache/result.log."
    )
    public static volatile boolean isSaveResult = false;

    /**
     * 是否打印parent类里的field
     */
    @Option(level = 1,
            name = "print-parent-fields",
            summary = "Option to print all fileds in parent class",
            description = "This option enables print files in parent class, default value true."
    )
    public static volatile boolean printParentFields = true;

    /**
     * 是否打开verbose 开关
     */
    @Option(level = 1,
            name = "verbose",
            summary = "Option to print verbose information",
            description = "This option enables print verbose information, default value false."
    )
    public static volatile boolean verbose = false;
}
