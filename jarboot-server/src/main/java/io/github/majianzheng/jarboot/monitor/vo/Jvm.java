package io.github.majianzheng.jarboot.monitor.vo;

import lombok.Data;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * JVM相关信息
 *
 * @author majianzheng
 */
@Data
public class Jvm {
    /**
     * 当前JVM占用的内存总数(M)
     */
    private double total;

    /**
     * JVM最大可用内存总数(M)
     */
    private double max;

    /**
     * JVM空闲内存(M)
     */
    private double free;

    /**
     * JDK版本
     */
    private String version;

    /**
     * JDK路径
     */
    private String home;

    private String name;

    /**
     * JDK启动时间
     */
    private Long startTime;

    /**
     * JDK运行时间
     */
    private Long runTime;

    /**
     * 运行参数
     */
    private String inputArgs;


    public Jvm() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        name = runtimeMXBean.getVmName();
        startTime = runtimeMXBean.getStartTime();
        runTime = System.currentTimeMillis() - startTime;
        inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments().toString();
    }
}
