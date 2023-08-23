package io.github.majianzheng.jarboot.demo.cmd;

import io.github.majianzheng.jarboot.api.cmd.annotation.*;
import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.api.cmd.spi.CommandProcessor;
import io.github.majianzheng.jarboot.demo.DemoServerApplication;

import java.util.concurrent.TimeUnit;

/**
 * fib算法命令
 * @author jianzhengma
 */
@Name("fib")
@Summary("The fib command summary")
@Description(" fib -n 10 -i 3 10 \n fib 10\n fib -n 1000 100")
public class FibCommandProcessor implements CommandProcessor {
    private int number = 1;
    private int interval = 0;
    private int value = 1;

    @Option(shortName = "n", longName = "number")
    @Description("执行次数")
    public void setNumber(int n) {
        this.number = n;
    }
    @Option(shortName = "i", longName = "interval")
    @Description("执行间隔时间（ms）")
    public void setInterval(int i) {
        this.interval = i;
    }
    @Argument(argName = "value", index = 0)
    @Description("输入参数")
    public void setValue(int v) {
        this.value = v;
    }

    @Override
    public String process(CommandSession session, String[] args) {
        session.console("开始执行fib算法>>>");
        StringBuilder sb = new StringBuilder();
        sb
                .append("执行次数:")
                .append(number)
                .append(", 执行间隔:")
                .append(interval);
        session.console(sb.toString());
        int result = 0;
        long b = System.currentTimeMillis();
        for (int i = 0; i < this.number; ++i) {
            if (this.interval > 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(this.interval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            result = DemoServerApplication.fib(this.value);
        }
        DemoServerApplication.notice("计算完成", session.getSessionId());
        return "计算结果：" + result + ", 耗时(ms)：" + (System.currentTimeMillis() - b);
    }

    @Override
    public void afterProcess(String result, Throwable e) {
        this.interval = 0;
        this.number = 1;
        this.value = 1;
    }
}
