package com.mz.jarboot.core.utils;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HtmlNodeUtilsTest {
    @Test
    public void testCreateNode() {
        String node = HtmlNodeUtils.createNode("span", "test", "red");
        assertEquals("<span style=\"color:red\">test</span>", node);
        node = HtmlNodeUtils.createNode("div", "test", "red");
        assertEquals("<div style=\"color:red\">test</div>", node);
    }

    @Test
    public void testCreateSpan() {
        String node = HtmlNodeUtils.createSpan("test", "red");
        assertEquals("<span style=\"color:red\">test</span>", node);
    }
}
