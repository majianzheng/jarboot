package com.mz.jarboot.core.cmd.impl;

import com.mz.jarboot.core.cmd.Command;
import com.mz.jarboot.core.cmd.ProcessHandler;
import com.mz.jarboot.core.constant.CoreConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jianzhengma
 */
public class JvmCommandImpl extends Command {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private String name;
    private ProcessHandler handler = null;
    public JvmCommandImpl(String name, String args) {
        this.name = name;
    }
    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public String name() {
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
        handler.console(this.name(), "暂未实现");
        //没有监控直接结束
        complete();
    }

    @Override
    public void complete() {
        if (null != handler) {
            handler.end(this.name());
        }
    }
}
