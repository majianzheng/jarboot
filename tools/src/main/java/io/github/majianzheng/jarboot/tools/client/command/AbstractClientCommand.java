package io.github.majianzheng.jarboot.tools.client.command;

import io.github.majianzheng.jarboot.api.cmd.annotation.Description;
import io.github.majianzheng.jarboot.api.cmd.annotation.Name;
import io.github.majianzheng.jarboot.api.cmd.annotation.Summary;
import io.github.majianzheng.jarboot.api.pojo.ServerRuntimeInfo;
import io.github.majianzheng.jarboot.client.ClientProxy;
import io.github.majianzheng.jarboot.client.ClusterOperator;
import io.github.majianzheng.jarboot.common.AnsiLog;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

/**
 * 客户端命令
 * @author majianzheng
 */
public abstract class AbstractClientCommand {
    protected String loginHost;
    protected String name;
    protected ClusterOperator client;
    protected Terminal terminal;
    protected ClientProxy proxy;
    protected LineReader lineReader;
    protected boolean clusterMode;
    protected ServerRuntimeInfo runtimeInfo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setClient(ClusterOperator client) {
        this.client = client;
    }

    public void setTerminal(Terminal terminal) {
        this.terminal = terminal;
    }

    public void setClientProxy(ClientProxy proxy) {
        this.proxy = proxy;
    }

    public void setLoginHost(String loginHost) {
        this.loginHost = loginHost;
    }

    public void setLineReader(LineReader lineReader) {
        this.lineReader = lineReader;
    }

    public void setClusterMode(boolean clusterMode) {
        this.clusterMode = clusterMode;
    }
    public void setRuntimeInfo(ServerRuntimeInfo runtimeInfo) {
        this.runtimeInfo = runtimeInfo;
    }

    /**
     * 执行命令
     */
    public abstract void run();

    /**
     * 取消命令
     */
    public abstract void cancel();

    public void printHelp() {
        this.printHelp(this.getClass());
    }

    protected String withDefault(String str) {
        return StringUtils.isEmpty(str) ? "-" : str;
    }

    protected String withDefault(String str, String defaultStr) {
        if (null == defaultStr) {
            defaultStr = "-";
        }
        return StringUtils.isEmpty(str) ? defaultStr : str;
    }

    protected void println(String str) {
        AnsiLog.println(str);
    }

    protected void printHelp(Class<?> cls) {
        AnsiLog.println("Usage:");
        Name cmd = cls.getAnnotation(Name.class);
        if (null != cmd) {
            AnsiLog.println("Command: " + cmd.value());
        }
        Summary summary = cls.getAnnotation(Summary.class);
        if (null != summary) {
            AnsiLog.println(summary.value());
        }
        Description description = cls.getAnnotation(Description.class);
        if (null != description) {
            AnsiLog.println(description.value());
        }
    }
}
