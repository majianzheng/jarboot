package com.mz.jarboot.dto;

import com.mz.jarboot.constant.ResultCodeConst;
import com.mz.jarboot.exception.MzException;

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
		if(e instanceof MzException) {
			MzException eTmp = (MzException)e;
			this.resultCode = eTmp.getErrorCode();
		}else {
			this.resultCode = ResultCodeConst.INTERNAL_ERROR;
		}
	}
	
}
