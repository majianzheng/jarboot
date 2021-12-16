package com.mz.jarboot.core.stream;

import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.ResponseType;
import com.mz.jarboot.core.basic.AgentServiceOperator;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.utils.LogUtils;
import com.mz.jarboot.core.utils.StringUtils;
import org.slf4j.Logger;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 标准输出流、错误流重定向反应器
 * @author majianzheng
 */
@SuppressWarnings("all")
public class StdOutStreamReactor {
    private static final Logger logger = LogUtils.getLogger();

    /** flush wait time */
    private static final int WAIT_TIME = 800;
    /** 标准输出流实现 */
    private final StdConsoleOutputStream consoleOutputStream;
    /** 默认的标准输出流备份 */
    private final PrintStream defaultOut;
    /** 默认的错误输出流备份 */
    private final PrintStream defaultErr;
    /** 自定义的标准输出流 */
    private final PrintStream stdOutPrintStream;
    /** 是否开启 */
    private boolean isOn = false;
    /** 上一次的打印时间 */
    private volatile long lastStdTime = 0;
    /** 启动完成判定时间 */
    private long startDetermineTime = 5000;
    /** 是否正在唤醒 */
    private final AtomicBoolean wakeuping = new AtomicBoolean(false);
    /** 监控终端输出的定时任务，负责判定是否启动完成 */
    private ScheduledFuture<?> watchFuture;

    /**
     * 标准输出流显示是否开启
     * @return 是否开启
     */
    public boolean isEnabled() {
        return isOn;
    }

    /**
     * 获取单例
     * @return 单例
     */
    public static StdOutStreamReactor getInstance() {
        return StdOutStreamReactorHolder.INSTANCE;
    }

    /**
     * 启动中开始，判定是否启动完成
     */
    public void setStarting() {
        consoleOutputStream.setPrintHandler(this::stdStartingPrint);
        lastStdTime = System.currentTimeMillis();
        //启动监控线程，监控间隔2秒
        final long delay = 2;
        watchFuture = EnvironmentContext
                .getScheduledExecutorService()
                .scheduleWithFixedDelay(this::determineStarted, delay, delay, TimeUnit.SECONDS);
    }

    /**
     * 开启、关闭终端输出显示
     * @param b 是否开启
     */
    public void enabled(boolean b) {
        if (b) {
            if (isOn) {
                return;
            }
            System.setOut(stdOutPrintStream);
            System.setErr(stdOutPrintStream);
            this.isOn = true;
        } else {
            if (this.isOn) {
                //恢复默认
                System.setErr(defaultErr);
                System.setOut(defaultOut);
                this.isOn = false;
            }
        }
    }

    private void enableColor() {
        try {
            Class<?> cls = Class.forName("com.mz.jarboot.common.AnsiLog");
            Field field = cls.getDeclaredField("enableColor");
            field.setAccessible(true);
            field.setBoolean(null, true);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 标准输出
     * @param text 文本
     */
    private void stdPrint(String text) {
        if (StringUtils.isEmpty(text)) {
            return;
        }
        CommandResponse resp = new CommandResponse();
        resp.setSuccess(true);
        resp.setResponseType(ResponseType.STD_PRINT);
        resp.setBody(text);
        resp.setSessionId(CommandConst.SESSION_COMMON);
        ResultStreamDistributor.write(resp);
    }

    /**
     * 标准输出，退格
     * @param num
     */
    private void stdBackspace(int num) {
        if (num > 0) {
            CommandResponse resp = new CommandResponse();
            resp.setSuccess(true);
            resp.setResponseType(ResponseType.BACKSPACE);
            resp.setBody(String.valueOf(num));
            resp.setSessionId(CommandConst.SESSION_COMMON);
            ResultStreamDistributor.write(resp);
        }
    }

    /**
     * 开始中标准输出
     * @param text 文本
     */
    private void stdStartingPrint(String text) {
        stdPrint(text);
        //更新计时
        lastStdTime = System.currentTimeMillis();
    }

    /**
     * 构造方法
     */
    private StdOutStreamReactor() {
        startDetermineTime = Long.getLong(CoreConstant.START_DETERMINE_TIME_KEY, 8000);
        consoleOutputStream = new StdConsoleOutputStream(this::onWakeup);
        //备份默认的输出流
        defaultOut = System.out;
        defaultErr = System.err;
        stdOutPrintStream = new PrintStream(consoleOutputStream, true);
        this.init();
        this.enableColor();
    }

    /**
     * 懒加载，私有内部类模式单例
     */
    private static class StdOutStreamReactorHolder {
        static StdOutStreamReactor INSTANCE = new StdOutStreamReactor();
    }

    /**
     * 初始化
     */
    private void init() {
        // 输出不满一行的字符串
        consoleOutputStream.setPrintHandler(this::stdPrint);
        //退格
        consoleOutputStream.setBackspaceHandler(this::stdBackspace);
        //默认开启
        this.enabled(true);
    }

    /**
     * 唤醒标准输出、错误流的IO刷新
     */
    private void onWakeup() {
        //io唤醒机制，当IO第一次变动时，等待一段时间后触发刷新，忽视等待期间的事件，然后开始新的一轮
        //检查是否正在等待weakup
        if (wakeuping.compareAndSet(false, true)) {
            return;
        }
        //启动延时任务，防抖动设计，忽视中间变化
        EnvironmentContext
                .getScheduledExecutorService()
                .schedule(this::flush, WAIT_TIME, TimeUnit.MILLISECONDS);
    }

    /**
     * IO刷新
     */
    private void flush() {
        //CAS判定，只有启动了weakup延迟后才可刷新
        if (wakeuping.compareAndSet(true, false)) {
            stdOutPrintStream.flush();
        }
    }

    /**
     * 判定是否启动完成
     */
    private void determineStarted() {
        if ((System.currentTimeMillis() - lastStdTime) < startDetermineTime) {
            return;
        }
        //超过一定时间没有控制台输出，判定启动成功
        consoleOutputStream.setPrintHandler(this::stdPrint);
        //通知Jarboot server启动完成
        try {
            AgentServiceOperator.setStarted();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (null != watchFuture) {
                //启动完成，取消计划任务
                watchFuture.cancel(true);
                watchFuture = null;
            }
        }
    }
}
