package com.mz.jarboot.core.session;

import com.mz.jarboot.core.constant.CoreConstant;

/**
 * 命令执行的结束状态
 * @author jianzhengma
 * 以下代码基于开源项目Arthas适配修改
 */
public class ExitStatus {
    private int statusCode;
    private String message = CoreConstant.EMPTY_STRING;

    /**
     * 命令执行成功的状态
     */
    public static final ExitStatus SUCCESS_STATUS = new ExitStatus(0);

    /**
     * 命令执行成功的状态
     * @return
     */
    public static ExitStatus success() {
        return SUCCESS_STATUS;
    }

    /**
     * 命令执行失败
     * @param statusCode
     * @param message
     * @return
     */
    public static ExitStatus failure(int statusCode, String message) {
        if (statusCode == 0) {
            throw new IllegalArgumentException("failure status code cannot be 0");
        }
        return new ExitStatus(statusCode, message);
    }

    /**
     * 判断是否为失败状态
     * @param exitStatus
     * @return
     */
    public static boolean isFailed(ExitStatus exitStatus) {
        return exitStatus != null && exitStatus.getStatusCode() != 0;
    }

    private ExitStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    private ExitStatus(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

}
