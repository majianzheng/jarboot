package com.mz.jarboot.core.cmd.view.element;

import java.util.HashMap;
import java.util.Map;

/**
 * 开发中，后端的html渲染轻量化改进
 */
public class HtmlElement {
    private String tagName;
    private String innerHtml;
    private Map<String, String> styles = new HashMap<>();
    private Map<String, String> attr = new HashMap<>();
    public HtmlElement(String name) {
        this.tagName = name;
        this.innerHtml = "";
    }
    public HtmlElement(String name, String innerHtml) {
        this.tagName = name;
        this.innerHtml = innerHtml;
    }
    public HtmlElement attr(String key, String value) {
        attr.put(key, value);
        return this;
    }
    public HtmlElement color(String color) {
        return style("color", color);
    }
    public HtmlElement bold() {
        return style("font-weight", "bold");
    }

    public HtmlElement style(String k, String v) {
        styles.put(k, v);
        return this;
    }

    public HtmlElement setInnerHtml(String html) {
        this.innerHtml = html;
        return this;
    }

    public String toHtmlString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(tagName).append(" ")

                .append(innerHtml)
                .append("</").append(tagName).append(">")
        ;
        return sb.toString();
    }
}
