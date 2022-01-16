package com.mz.jarboot.event;

import com.mz.jarboot.api.event.JarbootEvent;

/**
 * 来自前端的函数调用事件
 * @author majianzheng
 */
public class FuncReceivedEvent implements JarbootEvent {
    private String service;
    private int func;
    private String sid;
    private String body;
    private String sessionId;

    public FuncReceivedEvent() {

    }

    public FuncReceivedEvent(FuncCode func, String sessionId) {
        this.func = func.ordinal();
        this.sessionId = sessionId;
    }

    public enum FuncCode {
        /** 执行命令func */
        CMD_FUNC,
        /** 取消执行命令func */
        CANCEL_FUNC,
        /** 信任主机func */
        TRUST_ONCE_FUNC,
        /** 检查是否信任主机func */
        CHECK_TRUSTED_FUNC,
        /** 断开诊断 */
        DETACH_FUNC,
        /** 连接关闭 */
        SESSION_CLOSED_FUNC,
        /** 无效 */
        FUNC_MAX
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public int getFunc() {
        return func;
    }

    public void setFunc(int func) {
        this.func = func;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public FuncCode funcCode() {
        final FuncCode[] values = FuncCode.values();
        if (func < 0 || func >= values.length) {
            return FuncCode.FUNC_MAX;
        }
        return values[func];
    }
}
