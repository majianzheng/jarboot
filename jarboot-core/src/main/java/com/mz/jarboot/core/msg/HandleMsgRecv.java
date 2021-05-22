package com.mz.jarboot.core.msg;

import com.mz.jarboot.core.constant.JarbootCoreConstant;

public class HandleMsgRecv {
    private String host;
    public HandleMsgRecv(String host) {
        this.host = host;
    }
    public void onMsgRecv(MsgRecv recv) {
        System.out.println("收到命令：" + recv.getCmd());
        switch (recv.getCmd()) {
            case JarbootCoreConstant.EXIT_CMD:
                this.handleExit();
                break;
            case JarbootCoreConstant.GET_MEM_INFO_CMD:
                this.handleMemory();
                break;
            case JarbootCoreConstant.THREAD_CMD:
                this.handleThread();
                break;
            default:
                //do nothing
                break;
        }
    }
    private void handleExit() {
        System.exit(0);
    }
    private void handleMemory() {
        //TODO 待开发
    }
    private void handleThread() {
        //TODO 待开发
    }
}
