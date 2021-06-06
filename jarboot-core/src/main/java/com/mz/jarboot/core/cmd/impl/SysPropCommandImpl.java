package com.mz.jarboot.core.cmd.impl;

import com.mz.jarboot.core.cmd.Command;
import com.mz.jarboot.core.session.CommandSession;
import com.mz.jarboot.core.cmd.model.SysPropModel;
import com.mz.jarboot.core.constant.CoreConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * show the jvm detail
 * @author jianzhengma
 */
public class SysPropCommandImpl extends Command {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private CommandSession handler = null;
    private SysPropModel model = new SysPropModel();

    @Override
    public boolean isRunning() {
        return null != handler && handler.isRunning();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void cancel() {
        //do nothing
    }

    @Override
    public void run(CommandSession handler) {
        this.handler = handler;

        logger.info("{} 开始执行》》》》", name);
        if (null == args || args.isEmpty()) {
            model.setProps(System.getProperties());
            handler.appendResult(model);
            complete();
            return;
        }
        String[] s = args.split(" ");
        String key = s[0].trim();
        String value = System.getProperty(key, null);
        if (s.length == 1) {
            //获取单个属性
            if (null == value) {
                handler.console("获取属性失败，系统中不存在该属性！");
            } else {
                model.addProp(key, value);
                handler.appendResult(model);
            }
        } else {
            //修改单个属性
            if (null == value) {
                handler.console("修改失败，系统中不存在该属性！");
            } else {
                value = s[1].trim();
                System.setProperty(key, value);
                handler.console("修改成功！");
                handler.console(key + "=" + value);
            }
        }
        complete();
    }

    @Override
    public void complete() {
        if (null != handler) {
            handler.end();
        }
    }
}
