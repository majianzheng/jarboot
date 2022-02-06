package com.mz.jarboot.common.pojo;

/**
 * http响应通用包装对象
 * @author majianzheng
 */
public abstract class BaseResponse {
	/**默认成功*/
	protected int resultCode = ResultCodeConst.SUCCESS;
	protected String resultMsg;
	protected Long total;

	protected BaseResponse() {
		
	}
	
	public int getResultCode() {
		return resultCode;
	}
	
	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}

	public String getResultMsg() {
		return resultMsg;
	}

	public void setResultMsg(String resultMsg) {
		this.resultMsg = resultMsg;
	}

	public Long getTotal() {
		return total;
	}

	public void setTotal(Long total) {
		this.total = total;
	}
}
