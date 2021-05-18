package com.mz.jarboot.exception;

import com.mz.jarboot.constant.ResultCodeConst;

public class MzException extends RuntimeException {
	private static final long serialVersionUID = -6230029731717806830L;

	private int errorCode;

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public MzException(String message) {
		super(message);
		this.errorCode = ResultCodeConst.OTHER;
	}

	public MzException() {
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
	}

}
