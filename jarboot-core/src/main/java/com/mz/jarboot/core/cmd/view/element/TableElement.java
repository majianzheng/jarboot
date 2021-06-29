package com.mz.jarboot.core.cmd.view.element;


import com.mz.jarboot.core.cmd.view.ViewRenderUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 开发中，后端的html渲染轻量化改进
 */
public class TableElement extends Element {
    private String title;
    private String[] header;
    private List<List<String>> rows = new ArrayList<>();
    private int border;

    public TableElement() {
        this(1);
    }

    public TableElement(int border) {
        this.border = border;
    }

    public TableElement title(String title) {
        this.title = title;
        return this;
    }
    public TableElement row(java.lang.String... cols) {
        this.row(false, cols);
        return this;
    }

    public TableElement row(Element... cols) {
        if (null != cols && cols.length > 0) {
            this.row(false, Arrays.stream(cols).map(Element::toHtml).toArray(value -> new String[cols.length]));
        }
        return this;
    }

    public TableElement row(boolean header, java.lang.String... cols) {
        if (null != cols && cols.length > 0) {
            if (header) {
                this.header = cols;
            } else {
                rows.add(Arrays.asList(cols));
            }
        }
        return this;
    }

    @Override
    public String toHtml() {
        List<String> h = null == header ? new ArrayList<>() : Arrays.asList(header);
        return ViewRenderUtil.renderTable(h, rows, title, this.border);
    }
}
