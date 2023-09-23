package io.github.majianzheng.jarboot.api.constant;

/**
 * 任务的生命周期
 * @author majianzheng
 */
public enum TaskLifecycle {
    /** 启动前 */
    PRE_START,
    /** 启动后 */
    AFTER_STARTED,
    /** 启动失败 */
    START_FAILED,
    /** 停止前 */
    PRE_STOP,
    /** 停止后 */
    AFTER_STOPPED,
    /** 停止失败 */
    STOP_FAILED,
    /** 异常离线 */
    EXCEPTION_OFFLINE,
    /** 完成 */
    FINISHED,
    /** 计划中 */
    SCHEDULING,
}
