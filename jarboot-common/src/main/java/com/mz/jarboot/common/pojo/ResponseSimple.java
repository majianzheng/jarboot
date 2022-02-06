package com.mz.jarboot.common.pojo;

import com.mz.jarboot.common.JarbootException;

/**
 * api响应数据基类
 * @author majianzheng
 */
public class ResponseSimple extends BaseResponse{
	
	public ResponseSimple() {
		
	}
	
	public ResponseSimple(int resultCode){
		this.resultCode = resultCode;
	}
	
	public ResponseSimple(int resultCode, String resultMsg) {
		this.resultCode = resultCode;
		this.resultMsg = resultMsg;
	}

	public ResponseSimple(Throwable e) {
		this.resultMsg = e.getMessage();
		if(e instanceof JarbootException) {
			JarbootException eTmp = (JarbootException)e;
			this.resultCode = eTmp.getErrorCode();
		}else {
			this.resultCode = ResultCodeConst.INTERNAL_ERROR;
		}
	}
	
}
