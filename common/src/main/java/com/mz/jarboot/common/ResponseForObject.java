package com.mz.jarboot.common;


/**
 * 返回的result字段存放了单个对象的response
 * @author majianzheng
 *
 * @param <T> 对象类型
 */
public class ResponseForObject<T> extends ResponseSimple{
	private T result;

	public ResponseForObject() {
		super();
	}
	
	public ResponseForObject(T result) {
		this.result = result;
	}
	
	public ResponseForObject(Throwable e) {
		this.resultMsg = e.getMessage();
		if(e instanceof MzException) {
			MzException eTmp = (MzException)e;
			this.resultCode = eTmp.getErrorCode();
		}else {
			this.resultCode = ResultCodeConst.INTERNAL_ERROR;
		}
	}
	
	public ResponseForObject(int resultCode, String resultMsg) {
		this.resultCode = resultCode;
		this.resultMsg = resultMsg;
	}
	
	public ResponseForObject(int resultCode, String resultMsg, T result) {
		this.resultCode = resultCode;
		this.resultMsg = resultMsg;
		this.result = result;
	}

	public T getResult() {
		return result;
	}

	public void setResult(T result) {
		this.result = result;
	}
}
