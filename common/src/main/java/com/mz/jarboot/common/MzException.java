package com.mz.jarboot.common;

public class MzException extends RuntimeException {
	private static final long serialVersionUID = -6230029731717806830L;

	private final int errorCode;

	public int getErrorCode() {
		return errorCode;
	}

	public MzException(String message) {
		super(message);
		errorCode = ResultCodeConst.INTERNAL_ERROR;
	}

	public MzException() {
		errorCode = ResultCodeConst.INTERNAL_ERROR;
	}

	public MzException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public MzException(int errorCode, Throwable cause) {
		super(cause);
		this.errorCode = errorCode;
	}

	public MzException(int errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	public MzException(String message, Throwable cause) {
		super(message, cause);
		errorCode = ResultCodeConst.INTERNAL_ERROR;
	}

}
