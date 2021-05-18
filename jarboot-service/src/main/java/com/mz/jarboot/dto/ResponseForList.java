package com.mz.jarboot.dto;


import com.mz.jarboot.constant.ResultCodeConst;
import com.mz.jarboot.exception.MzException;

import java.util.List;

/**
 * 返回的result字段存放了对象列表的response
 * @author majianzheng
 *
 * @param <T> 对象类型
 */
public class ResponseForList<T> extends ResponseSimple{
	private List<T> result;

	public ResponseForList() {
		super();
	}
	
	public ResponseForList(List<T> result) {
		this.result = result;
	}
	
	public ResponseForList(Throwable e) {
		this.resultMsg = e.getMessage();
		if(e instanceof MzException) {
			MzException eTmp = (MzException)e;
			this.resultCode = eTmp.getErrorCode();
		}else {
			this.resultCode = ResultCodeConst.INTERNAL_ERROR;
		}
	}
	
	public ResponseForList(List<T> result, Long total) {
		this.result = result;
		this.total = total;
	}

	public ResponseForList(List<T> result, int total) {
		this.result = result;
		this.total = (long) total;
	}
	
	public ResponseForList(int resultCode, String resultMsg) {
		this.resultCode = resultCode;
		this.resultMsg = resultMsg;
	}
	
	public ResponseForList(int resultCode, String resultMsg, List<T> result) {
		this.resultCode = resultCode;
		this.resultMsg = resultMsg;
		this.result = result;
	}

	public List<T> getResult() {
		return result;
	}

	public void setResult(List<T> result) {
		this.result = result;
	}
	
}
