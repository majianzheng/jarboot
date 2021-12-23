package com.mz.jarboot.core.cmd.view.element;

import com.mz.jarboot.core.utils.HtmlNodeUtils;
import com.mz.jarboot.common.utils.StringUtils;

/**
 * @author majianzheng
 */
public class Element {
    protected String text;
    protected String color;

    public Element() {
        this(StringUtils.EMPTY);
    }

    public Element(String text) {
        this.text = text;
        this.color = StringUtils.EMPTY;
    }

    public Element setText(String text) {
        this.text = text;
        return this;
    }

    public Element color(String color) {
        this.color = color;
        return this;
    }

    public String toHtml() {
        if (null == this.text) {
            return StringUtils.EMPTY;
        }
        if (this.color.isEmpty()) {
            return this.text;
        }
        return HtmlNodeUtils.span(this.text, this.color);
    }
}
