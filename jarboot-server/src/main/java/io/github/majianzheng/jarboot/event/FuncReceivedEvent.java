package io.github.majianzheng.jarboot.event;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.common.pojo.FuncRequest;

/**
 * 来自前端的函数调用事件
 * @author majianzheng
 */
public class FuncReceivedEvent extends FuncRequest implements JarbootEvent {
    private String sessionId;
    private int cols;
    private int rows;

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

        /** 信任主机func */
        TRUST_ALWAYS_FUNC,

        /** 连接关闭 */
        SESSION_CLOSED_FUNC,

        /** 打开窗口 */
        ACTIVE_WINDOW,

        /** 关闭窗口 */
        CLOSE_WINDOW,

        /** 无效 */
        FUNC_MAX
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

    public int getCols() {
        return cols;
    }

    public void setCols(int cols) {
        this.cols = cols;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    @Override
    public String toString() {
        return "FuncReceivedEvent{" +
                "service='" + service + '\'' +
                ", func=" + func +
                ", sid='" + sid + '\'' +
                ", body='" + body + '\'' +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }
}
