package com.mz.jarboot.core.cmd;


import com.mz.jarboot.api.cmd.spi.CommandProcessor;

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

    @SuppressWarnings("all")
    @Override
    public void run() {
        String result = "";
        Throwable throwable = null;
        try {
            result = processor.process(session, args);
            session.end(true, result);
        } catch (Throwable e) {
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
}
