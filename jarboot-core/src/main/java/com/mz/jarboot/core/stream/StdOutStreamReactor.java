package com.mz.jarboot.core.stream;

import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.ResponseType;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.constant.CoreConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("all")
public class StdOutStreamReactor {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private final StdConsoleOutputStream sos;
    private final PrintStream defaultOut;
    private final PrintStream defaultErr;
    private final PrintStream stdRedirectStream;
    private volatile boolean isOn = false;
    private volatile long lastStdTime = 0;
    private long startDetermineTime = 5000;
    private final StdPrintHandler stdoutPrintHandler = text -> stdConsole(text);
    private final StdPrintHandler startingPrintHandler = text -> stdStartingConsole(text);

    private void stdConsole(String text) {
        CommandResponse resp = new CommandResponse();
        resp.setSuccess(true);
        resp.setResponseType(ResponseType.STD_OUT);
        resp.setBody(text);
        resp.setSessionId(CommandConst.SESSION_COMMON);
        EnvironmentContext.distribute(resp);
    }

    private void stdStartingConsole(String text) {
        stdConsole(text);
        //更新计时
        lastStdTime = System.currentTimeMillis();
    }

    private StdOutStreamReactor() {
        startDetermineTime = Long.getLong(CoreConstant.START_DETERMINE_TIME_KEY, 5000);
        sos = new StdConsoleOutputStream();
        //备份默认的输出流
        defaultOut = System.out;
        defaultErr = System.err;
        stdRedirectStream = new PrintStream(sos);
        this.init();
    }
    //懒加载，私有内部类模式单例
    private static class StdOutStreamReactorHolder {
        static StdOutStreamReactor inst = new StdOutStreamReactor();
    }

    private void init() {
        // 输出不满一行的字符串
        sos.setPrintHandler(stdoutPrintHandler);
        //输出行
        sos.setPrintLineHandler(stdoutPrintHandler);
        //默认开启
        this.enabled(true);
    }

    public void enabled(boolean b) {
        if (b) {
            if (isOn) {
                return;
            }
            System.setOut(stdRedirectStream);
            System.setErr(stdRedirectStream);
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

    public boolean isEnabled() {
        return isOn;
    }

    public static StdOutStreamReactor getInstance() {
        return StdOutStreamReactorHolder.inst;
    }

    public void setStarting() {
        sos.setPrintLineHandler(startingPrintHandler);
        lastStdTime = System.currentTimeMillis();
        //启动监控线程
        EnvironmentContext.getScheduledExecutorService().execute(() -> {
            do {
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } while ((System.currentTimeMillis() - lastStdTime) < startDetermineTime);
            //超过一定时间没有控制台输出，判定启动成功
            sos.setPrintLineHandler(stdoutPrintHandler);
            //通知Jarboot server启动完成
            try {
                EnvironmentContext.setStarted();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
    }
}
