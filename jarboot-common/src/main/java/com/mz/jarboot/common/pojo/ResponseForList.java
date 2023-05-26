package com.mz.jarboot.common.pojo;


import com.mz.jarboot.common.JarbootException;

import java.util.List;

/**
 * 返回的result字段存放了对象列表的response
 * @author majianzheng
 *
 * @param <T> 对象类型
 */
public class ResponseForList<T> extends ResponseSimple {
	private List<T> data;
	private Long total;
	public ResponseForList() {
		super();
	}
	
	public ResponseForList(List<T> data) {
		this.data = data;
	}
	
	public ResponseForList(Throwable e) {
		this.msg = e.getMessage();
		if(e instanceof JarbootException) {
			JarbootException eTmp = (JarbootException)e;
			this.code = eTmp.getErrorCode();
		}else {
			this.code = ResultCodeConst.INTERNAL_ERROR;
		}
	}
	
	public ResponseForList(List<T> data, Long total) {
		this.data = data;
		this.total = total;
	}

	public ResponseForList(List<T> data, int total) {
		this.data = data;
		this.total = (long) total;
	}
	
	public ResponseForList(int resultCode, String resultMsg) {
		this.code = resultCode;
		this.msg = resultMsg;
	}
	
	public ResponseForList(int resultCode, String resultMsg, List<T> data) {
		this.code = resultCode;
		this.msg = resultMsg;
		this.data = data;
	}

	public List<T> getData() {
		return data;
	}

	public void setData(List<T> data) {
		this.data = data;
	}

	public Long getTotal() {
		return total;
	}

	public void setTotal(Long total) {
		this.total = total;
	}
}
