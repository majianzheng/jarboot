package com.mz.jarboot.core.cmd.impl;

import com.mz.jarboot.core.cmd.Command;
import com.mz.jarboot.core.cmd.ProcessHandler;
import com.mz.jarboot.core.constant.CoreConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;

/**
 * show the jvm detail
 * @author jianzhengma
 */
public class JvmCommandImpl extends Command {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private ProcessHandler handler = null;

    private void println(String text) {
        if (null != handler) {
            handler.console(text);
        }
    }

    private void printSplit() {
        println("---------------------------------------------------");
    }

    private void showRuntime() {
        RuntimeMXBean mxb = ManagementFactory.getRuntimeMXBean();
        println("\nRUNTIME");
        printSplit();
        println("MACHINE-NAME:" + mxb.getName());
        println("JVM-START-TIME: " + mxb.getStartTime());
        println("MANAGEMENT-SPEC-VERSION: " + mxb.getManagementSpecVersion());
        println("SPEC-NAME: " + mxb.getSpecName());
        println("SPEC-VENDOR: " + mxb.getSpecVendor());
        println("SPEC-VERSION: " + mxb.getSpecVersion());
        println("VM-NAME: " + mxb.getVmName());
        println("VM-VENDOR: " + mxb.getVmVendor());
        println("VM-VERSION: " + mxb.getVmVersion());
        println("INPUT-ARGUMENTS: " + mxb.getInputArguments());
        println("CLASS-PATH: " + mxb.getClassPath());
        println("BOOT-CLASS-PATH: " + mxb.getBootClassPath());
        println("LIBRARY-PATH: " + mxb.getLibraryPath());
    }

    private void showSystemDetail() {
        println("\nOPERATING-SYSTEM");
        printSplit();
        OperatingSystemMXBean osb = ManagementFactory.getOperatingSystemMXBean();
        println("OS: " + osb.getName()); //Windows 10
        println("ARCH: " + osb.getArch()); //amd 64
        println("PROCESSORS-COUNT: " + osb.getAvailableProcessors()); //4
        println("VERSION: " + osb.getVersion()); //10.0
        println("LOAD-AVERAGE: " + osb.getSystemLoadAverage()); //-1.0
    }
    private void showHeap() {
        println("\nMEMORY");
        printSplit();
        MemoryMXBean mxb = ManagementFactory.getMemoryMXBean();
        //Heap
        println("Max:" + mxb.getHeapMemoryUsage().getMax() / 1024 / 1024 + "MB");    //Max
        println("Init:" + mxb.getHeapMemoryUsage().getInit() / 1024 / 1024 + "MB");  //Init
        println("Committed:" + mxb.getHeapMemoryUsage().getCommitted() / 1024 / 1024 + "MB");   //Committed
        println("Used:" + mxb.getHeapMemoryUsage().getUsed() / 1024 / 1024 + "MB");  //Used:7MB
        println(mxb.getHeapMemoryUsage().toString());    //init = 132120576(129024K) used = 8076528(7887K) committed = 126877696(123904K) max = 1862270976(1818624K)

        //Non heap
        println("Max:" + mxb.getNonHeapMemoryUsage().getMax() / 1024 / 1024 + "MB");    //Max:0MB
        println("Init:" + mxb.getNonHeapMemoryUsage().getInit() / 1024 / 1024 + "MB");  //Init:2MB
        println("Committed:" + mxb.getNonHeapMemoryUsage().getCommitted() / 1024 / 1024 + "MB");   //Committed:8MB
        println("Used:" + mxb.getNonHeapMemoryUsage().getUsed() / 1024 / 1024 + "MB");  //Used:7MB
        println(mxb.getNonHeapMemoryUsage().toString());    //init = 2555904(2496K) used = 7802056(7619K) committed = 9109504(8896K) max = -1(-1K)
    }
    @Override
    public boolean isRunning() {
        return null != handler && !handler.isEnded();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void cancel() {
        //do nothing
    }

    @Override
    public void run(ProcessHandler handler) {
        this.handler = handler;
        logger.info("jvm 开始执行》》》》{}", name);
        showRuntime();
        showSystemDetail();
        showHeap();
        //没有监控直接结束
        complete();
    }

    @Override
    public void complete() {
        if (null != handler) {
            handler.end();
        }
    }
}
