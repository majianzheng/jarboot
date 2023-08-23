package io.github.majianzheng.jarboot.common.pojo;

import java.util.List;

/**
 * 返回的result字段存放了对象列表的response
 * @author majianzheng
 *
 * @param <T> 对象类型
 */
public class PagedList<T> {
	private List<T> rows;
	private Long total;
	public PagedList() {}
	
	public PagedList(List<T> rows) {
		this.rows = rows;
		this.total = (long) rows.size();
	}
	
	public PagedList(List<T> rows, Long total) {
		this.rows = rows;
		this.total = total;
	}

	public PagedList(List<T> rows, int total) {
		this.rows = rows;
		this.total = (long) total;
	}

	public List<T> getRows() {
		return rows;
	}

	public void setRows(List<T> rows) {
		this.rows = rows;
	}

	public Long getTotal() {
		return total;
	}

	public void setTotal(Long total) {
		this.total = total;
	}
}
