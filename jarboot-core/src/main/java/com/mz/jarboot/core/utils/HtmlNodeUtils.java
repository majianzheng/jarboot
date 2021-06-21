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
    public static String red(String text) {
        return createSpan(text, "red");
    }
    public static String green(String text) {
        return createSpan(text, "green");
    }
    public static String magenta(String text) {
        return createSpan(text, "magenta");
    }
    private HtmlNodeUtils() {}
}
