package com.mz.jarboot.core.stream;

import com.mz.jarboot.core.session.CommandSession;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class StdOutStreamReactor {
    private StdConsoleOutputStream sos;
    private Map<String, CommandSession> reg;
    private final PrintStream defaultOut;
    private final PrintStream defaultErr;
    private final PrintStream stdRedirectStream;
    private volatile boolean isOn = false;
    private StdOutStreamReactor() {
        reg = new HashMap<>();
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
        sos.setPrintHandler(text -> {
            //暂不支持
        });
        //输出行
        sos.setPrintLineHandler(text -> {
            Collection<CommandSession> sessions = reg.values().stream()
                    .filter(session -> !session.isRunning()).collect(Collectors.toList());
            sessions.forEach(session -> session.console(text));
        });
    }

    private void redirectStd() {
        if (isOn) {
            return;
        }
        System.setOut(stdRedirectStream);
        System.setErr(stdRedirectStream);
        this.isOn = true;
    }

    private void reset() {
        if (this.isOn && reg.isEmpty()) {
            //恢复默认
            System.setErr(defaultErr);
            System.setOut(defaultOut);
            this.isOn = false;
        }
    }

    public static StdOutStreamReactor getInstance() {
        return StdOutStreamReactorHolder.inst;
    }

    public synchronized boolean isRegistered(String sessionId) {
        return reg.containsKey(sessionId);
    }

    public synchronized void register(CommandSession session) {
        reg.put(session.getSessionId(), session);
        redirectStd();
    }

    public synchronized void unRegister(String sessionId) {
        reg.remove(sessionId);
        reset();
    }

    public synchronized void unRegisterAll() {
        reg.clear();
        reset();
    }

}
