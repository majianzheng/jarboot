package io.github.majianzheng.jarboot.common.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author majianzheng
 */
public class CommandCliParserTest {
    @Test
    public void testSplitArgs() {
        List<String> list = CommandCliParser.splitArgs("abc def \"1 \\\"234 5\\\" \" xyz");
        Assert.assertEquals(4, list.size());
        Assert.assertEquals("abc", list.get(0));
        Assert.assertEquals("def", list.get(1));
        Assert.assertEquals("1 \\\"234 5\\\" ", list.get(2));
        Assert.assertEquals("xyz", list.get(3));

        list = CommandCliParser.splitArgs("abc def   xyz");
        Assert.assertEquals("abc", list.get(0));
        Assert.assertEquals("def", list.get(1));
        Assert.assertEquals("xyz", list.get(2));

        list = CommandCliParser.splitArgs("abc \"def   x yz\"");
        Assert.assertEquals("abc", list.get(0));
        Assert.assertEquals("def   x yz", list.get(1));

        list = CommandCliParser.splitArgs("abc \"def   x yz\" jarboot");
        Assert.assertEquals("abc", list.get(0));
        Assert.assertEquals("def   x yz", list.get(1));
        Assert.assertEquals("jarboot", list.get(2));


        list = CommandCliParser.splitArgs("   abc \"def   x yz\" jarboot    ");
        Assert.assertEquals("abc", list.get(0));
        Assert.assertEquals("def   x yz", list.get(1));
        Assert.assertEquals("jarboot", list.get(2));
    }
}
