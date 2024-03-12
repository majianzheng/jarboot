package io.github.majianzheng.jarboot.core.stream;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.common.notify.NotifyReactor;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.core.basic.AgentServiceOperator;
import io.github.majianzheng.jarboot.core.basic.EnvironmentContext;
import io.github.majianzheng.jarboot.core.constant.CoreConstant;
import io.github.majianzheng.jarboot.core.event.StdoutAppendEvent;
import io.github.majianzheng.jarboot.core.utils.LogUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.*;
import java.lang.reflect.Field;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 标准输出流、错误流重定向反应器
 * @author majianzheng
 */
@SuppressWarnings({"squid:S106", "squid:S1181"})
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
    private final long startDetermineTime;
    /** 是否正在唤醒 */
    private final AtomicBoolean wakeup = new AtomicBoolean(false);
    /** 监控终端输出的定时任务，负责判定是否启动完成 */
    private ScheduledFuture<?> watchFuture;
    /** std文件输出 */
    private FileOutputStream stdoutFileStream = null;
    /** std事件订阅 */
    private final Subscriber<StdoutAppendEvent> subscriber;

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
                .getScheduledExecutor()
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
            if (this.isStdoutFileAlways()) {
                this.openStdoutFileStream();
            }
            this.isOn = true;
        } else {
            if (this.isOn) {
                //恢复默认
                System.setErr(defaultErr);
                System.setOut(defaultOut);
                this.closeStdFileStreamQuietly();
                this.isOn = false;
            }
        }
    }

    @SuppressWarnings("java:S3011")
    private void enableAnsiLogColor() {
        try {
            Class<?> cls = Class.forName("io.github.majianzheng.jarboot.common.AnsiLog");
            Field field = cls.getDeclaredField("enableColor");
            field.setAccessible(true);
            field.setBoolean(null, true);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 开始中标准输出
     * @param text 文本
     */
    private void stdStartingPrint(String text) {
        this.stdPrint(text);
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
        subscriber = new Subscriber<StdoutAppendEvent>() {
            @Override
            public void onEvent(StdoutAppendEvent event) {
                if (null != stdoutFileStream) {
                    try {
                        stdoutFileStream.write(event.getText().getBytes());
                    } catch (Exception e) {
                        logger.debug("write stdout file failed, will close stdout file.", e);
                        closeStdFileStreamQuietly();
                    }
                }
            }

            @Override
            public Class<? extends JarbootEvent> subscribeType() {
                return StdoutAppendEvent.class;
            }
        };
        this.init();
        this.openStdoutFileStream();
        this.enableAnsiLogColor();
    }

    /**
     * 懒加载，私有内部类模式单例
     */
    private static class StdOutStreamReactorHolder {
        static final StdOutStreamReactor INSTANCE = new StdOutStreamReactor();
    }

    private void openStdoutFileStream() {
        //先关闭旧文件
        this.closeStdFileStreamQuietly();
        String fileName = System.getProperty(CoreConstant.STD_OUT_FILE);
        if (StringUtils.isEmpty(fileName)) {
            return;
        }
        File file = new File(fileName);
        try {
            if (file.exists()) {
                if (!file.isFile()) {
                    logger.warn("stdout file {} is exists and is not a file!", fileName);
                    return;
                }
                FileUtils.deleteQuietly(file);
            }
            if (!file.createNewFile()) {
                logger.error("create stdout file failed. file: {}", fileName);
                return;
            }
            stdoutFileStream = new FileOutputStream(file);
            NotifyReactor.getInstance().registerSubscriber(this.subscriber);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }

    /**
     * 初始化
     */
    private void init() {

        // 输出不满一行的字符串
        consoleOutputStream.setPrintHandler(this::stdPrint);
        //默认开启
        this.enabled(true);
    }

    private void stdPrint(String text) {
        NotifyReactor.getInstance().publishEvent(new StdoutAppendEvent(text));
    }

    private void closeStdFileStreamQuietly() {
        if (null != stdoutFileStream) {
            NotifyReactor.getInstance().deregisterSubscriber(this.subscriber);
            try {
                stdoutFileStream.close();
            } catch (Exception e) {
                //ignore
            } finally {
                stdoutFileStream = null;
            }
        }
    }

    private boolean isStdoutFileAlways() {
        return Boolean.getBoolean(CoreConstant.STD_OUT_FILE_ALWAYS);
    }

    /**
     * 唤醒标准输出、错误流的IO刷新
     */
    private void onWakeup() {
        //io唤醒机制，当IO第一次变动时，等待一段时间后触发刷新，忽视等待期间的事件，然后开始新的一轮
        //检查是否正在等待wakeup
        if (wakeup.compareAndSet(false, true)) {
            return;
        }
        //启动延时任务，防抖动设计，忽视中间变化
        EnvironmentContext
                .getScheduledExecutor()
                .schedule(this::flush, WAIT_TIME, TimeUnit.MILLISECONDS);
    }

    /**
     * IO刷新
     */
    private void flush() {
        //CAS判定，只有启动了wakeup延迟后才可刷新
        if (wakeup.compareAndSet(true, false)) {
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
                watchFuture.cancel(false);
                watchFuture = null;
            }
            if (!this.isStdoutFileAlways()) {
                this.closeStdFileStreamQuietly();
            }
        }
    }
}
