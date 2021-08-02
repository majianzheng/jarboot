package com.mz.jarboot.demo.cmd;

import com.mz.jarboot.api.cmd.annotation.Description;
import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.annotation.Summary;
import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.api.cmd.spi.CommandProcessor;
import java.lang.instrument.Instrumentation;

/**
 * 测试jdk SPI
 * @author jianzhengma
 */
@Name("demo")
@Summary("The demo command summary")
@Description("The demo spi command usage detail")
public class DemoCommandProcessor implements CommandProcessor {
    private String server;

    @Override
    public void postConstruct(Instrumentation instrumentation, String server) {
        this.server = server;
    }

    @Override
    public String process(CommandSession session, String[] args) {
        StringBuilder sb = new StringBuilder();
        sb
                .append("Demo JDK SPI command process success.\nserver:")
                .append(server)
                .append(", args:");
        if (null != args && args.length > 0) {
            for (String a : args) {
                sb.append(a).append(", ");
            }
        }
        return sb.toString();
    }
}
