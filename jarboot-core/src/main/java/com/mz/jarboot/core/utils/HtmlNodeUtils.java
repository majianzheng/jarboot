package com.mz.jarboot.core.utils;

public class HtmlNodeUtils {
    public static String createSpan(String text, String color) {
        return createNode("span", text, color);
    }
    public static String createNode(String n, String text, String color) {
        StringBuilder builder = new StringBuilder();
        builder.append('<').append(n).append(" style=\"color:").append(color)
                .append("\">").append(text).append("</").append(n).append(">")
        ;
        return builder.toString();
    }
    private HtmlNodeUtils() {}
}
