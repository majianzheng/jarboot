package io.github.majianzheng.jarboot.core.utils;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author majianzheng
 */
public class HtmlNodeUtilsTest {
    @Test
    public void testCreateNode() {
        String node = HtmlNodeUtils.element("span", "test", "red");
        assertEquals("<span style=\"color:red\">test</span>", node);
        node = HtmlNodeUtils.element("div", "test", "red");
        assertEquals("<div style=\"color:red\">test</div>", node);
    }

    @Test
    public void testCreateSpan() {
        String node = HtmlNodeUtils.span("test", "red");
        assertEquals("<span style=\"color:red\">test</span>", node);
    }
}
