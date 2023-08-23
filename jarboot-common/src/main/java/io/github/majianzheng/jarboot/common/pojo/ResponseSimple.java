package io.github.majianzheng.jarboot.common.pojo;

import io.github.majianzheng.jarboot.common.JarbootException;

/**
 * api响应数据基类
 * @author majianzheng
 */
public class ResponseSimple extends BaseResponse{
	
	public ResponseSimple() {
		
	}
	
	public ResponseSimple(int code){
		this.code = code;
	}
	
	public ResponseSimple(int code, String resultMsg) {
		this.code = code;
		this.msg = resultMsg;
	}

	public ResponseSimple(String resultMsg) {
		this.code = -1;
		this.msg = resultMsg;
	}

	public ResponseSimple(Throwable e) {
		this.msg = e.getMessage();
		if(e instanceof JarbootException) {
			JarbootException eTmp = (JarbootException)e;
			this.code = eTmp.getErrorCode();
		}else {
			this.code = ResultCodeConst.INTERNAL_ERROR;
		}
	}
	
}
