package com.mz.jarboot.core.msg;

import com.alibaba.fastjson.JSON;
import com.mz.jarboot.common.Command;
import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.core.constant.JarbootCoreConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(JarbootCoreConstant.LOG_NAME);
    private String host;
    public CommandHandler(String host) {
        this.host = host;
    }
    public void onMsgRecv(String msg) {
        logger.debug("收到消息：{}", msg);
        Command recv;
        try {
            recv = JSON.parseObject(msg, Command.class);
        } catch (Exception e) {
            logger.warn("收到的消息格式错误", e);
            return;
        }
        logger.debug("开始执行命令：{}", recv.getCmd());
        switch (recv.getCmd()) {
            case CommandConst.EXIT_CMD:
                this.handleExit();
                break;
            case CommandConst.GET_MEM_INFO_CMD:
                this.handleMemory();
                break;
            case CommandConst.THREAD_CMD:
                this.handleThread();
                break;
            default:
                //do nothing
                break;
        }
    }
    private void handleExit() {
        logger.debug("执行exit");
        System.exit(0);
    }
    private void handleMemory() {
        //TODO 待开发
    }
    private void handleThread() {
        //TODO 待开发
    }
}
