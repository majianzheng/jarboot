package com.mz.jarboot.core.cmd.view.element;

import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.utils.HtmlNodeUtils;

/**
 * @author jianzhengma
 */
public class Element {
    protected String text;
    protected String color;

    public Element() {
        this(CoreConstant.EMPTY_STRING);
    }

    public Element(String text) {
        this.text = text;
        this.color = CoreConstant.EMPTY_STRING;
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
            return CoreConstant.EMPTY_STRING;
        }
        if (this.color.isEmpty()) {
            return this.text;
        }
        return HtmlNodeUtils.createSpan(this.text, this.color);
    }
}
