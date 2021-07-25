package com.mz.jarboot.common;

/**
 * 命令类型
 * @author majianzheng
 */
public enum CommandType {
    /**
     * 用户命令，前端或api发起的执行
     */
    USER_PUBLIC,
    /**
     * 系统内部的控制指令
     */
    INTERNAL,
    /**
     * 不支持的未知命令
     */
    UNKNOWN
}
