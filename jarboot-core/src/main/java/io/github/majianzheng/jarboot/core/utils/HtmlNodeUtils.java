package io.github.majianzheng.jarboot.core.utils;

/**
 * @author majianzheng
 */
public class HtmlNodeUtils {
    /**
     * 创建span节点
     * @param text 内容
     * @param color 颜色
     * @return html
     */
    public static String span(String text, String color) {
        return element("span", text, color);
    }

    /**
     * 创建html节点
     * @param n 节点名
     * @param text 内容
     * @param color 颜色
     * @return html
     */
    public static String element(String n, String text, String color) {
        StringBuilder builder = new StringBuilder();
        builder
                .append('<')
                .append(n)
                .append(" style=\"color:")
                .append(color)
                .append("\">")
                .append(text)
                .append("</")
                .append(n)
                .append(">")
        ;
        return builder.toString();
    }
    private HtmlNodeUtils() {}
}
