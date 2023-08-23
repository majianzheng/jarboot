package io.github.majianzheng.jarboot.common;

import io.github.majianzheng.jarboot.common.pojo.ResultCodeConst;

/**
 * 项目内异常类
 * @author majianzheng
 */
public class JarbootException extends RuntimeException {
	private static final long serialVersionUID = -6230029731717806830L;

	private final int errorCode;

	public int getErrorCode() {
		return errorCode;
	}

	public JarbootException(String message) {
		super(message);
		errorCode = ResultCodeConst.INTERNAL_ERROR;
	}
	public JarbootException(Throwable message) {
		super(message.getMessage());
		errorCode = -1;
	}

	public JarbootException() {
		errorCode = ResultCodeConst.INTERNAL_ERROR;
	}

	public JarbootException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public JarbootException(int errorCode, Throwable cause) {
		super(cause);
		this.errorCode = errorCode;
	}

	public JarbootException(int errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	public JarbootException(String message, Throwable cause) {
		super(message, cause);
		errorCode = ResultCodeConst.INTERNAL_ERROR;
	}

}
