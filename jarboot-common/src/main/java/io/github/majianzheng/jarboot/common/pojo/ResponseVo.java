package io.github.majianzheng.jarboot.common.pojo;


import io.github.majianzheng.jarboot.common.JarbootException;

/**
 * 返回的result字段存放了单个对象的response
 * @author majianzheng
 *
 * @param <T> 对象类型
 */
public class ResponseVo<T> extends ResponseSimple{
	private T data;

	public ResponseVo() {
		super();
	}
	
	public ResponseVo(T data) {
		this.data = data;
	}
	
	public ResponseVo(Throwable e) {
		this.msg = e.getMessage();
		if(e instanceof JarbootException) {
			JarbootException eTmp = (JarbootException)e;
			this.code = eTmp.getErrorCode();
		}else {
			this.code = ResultCodeConst.INTERNAL_ERROR;
		}
	}
	
	public ResponseVo(int resultCode, String resultMsg) {
		this.code = resultCode;
		this.msg = resultMsg;
	}
	
	public ResponseVo(int resultCode, String resultMsg, T data) {
		this.code = resultCode;
		this.msg = resultMsg;
		this.data = data;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}
}
