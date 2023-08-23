package io.github.majianzheng.jarboot.core.cmd;


import io.github.majianzheng.jarboot.api.cmd.spi.CommandProcessor;
import io.github.majianzheng.jarboot.common.AnsiLog;

/**
 * 扩展的命令，由jdk SPI、Spring SPI加载的用户自定义命令
 * @author majianzheng
 */
public class ExtendCommand extends AbstractCommand {
    private final CommandProcessor processor;
    private String[] args;
    public ExtendCommand(CommandProcessor processor) {
        this.processor = processor;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    @SuppressWarnings("squid:S1181")
    @Override
    public void run() {
        String result = "";
        Throwable throwable = null;
        try {
            result = processor.process(session, args);
            session.end(true, result);
        } catch (Throwable e) {
            AnsiLog.error(e);
            session.end(false, e.getMessage());
            throwable = e;
        } finally {
            processor.afterProcess(result, throwable);
        }
    }

    @Override
    public void printHelp() {
        if (null == this.processor) {
            return;
        }
        this.printHelp(this.processor.getClass());
    }
    
    /**
     * 取消执行
     */
    @Override
    public void cancel() {
        this.processor.onCancel();
        super.cancel();
    }
}
