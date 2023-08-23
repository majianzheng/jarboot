package io.github.majianzheng.jarboot.common.pojo;

/**
 * http响应通用包装对象
 * @author majianzheng
 */
public abstract class BaseResponse {
	/**默认成功*/
	protected int code = ResultCodeConst.SUCCESS;
	protected String msg;
	protected boolean success = true;

	protected BaseResponse() {
		
	}
	
	public int getCode() {
		return code;
	}
	
	public void setCode(int code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public boolean getSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}
}
