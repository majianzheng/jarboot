package io.github.majianzheng.jarboot.common.protocol;

import io.github.majianzheng.jarboot.api.constant.CommonConst;

/**
 * @author majianzheng
 */
public enum NotifyType {
    /** 提示 */
    INFO,
    /** 警告 */
    WARN,
    /** 错误 */
    ERROR,
    /** 控制台消息打印 */
    CONSOLE,
    /** 执行执行完成 */
    COMMAND_END,
    /** Json类型的执行结果 */
    JSON_RESULT;

    public String body(String text) {
        return this.ordinal() + CommonConst.COMMA_SPLIT + text;
    }
}
