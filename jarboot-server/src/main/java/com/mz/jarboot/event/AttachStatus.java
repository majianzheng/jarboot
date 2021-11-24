package com.mz.jarboot.event;

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
}
