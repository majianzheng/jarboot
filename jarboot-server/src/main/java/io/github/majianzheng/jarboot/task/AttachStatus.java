package io.github.majianzheng.jarboot.task;

/**
 * Attach状态
 * @author jianzhengma
 */

public enum AttachStatus {
    /**
     * 正在Attach
     */
    ATTACHING,

    /**
     * 已经Attach
     */
    ATTACHED,

    /**
     * 已经退出
     */
    EXITED,

    /**
     * 不被信任的
     */
    NOT_TRUSTED,

    /**
     * 信任的
     */
    TRUSTED,
}
