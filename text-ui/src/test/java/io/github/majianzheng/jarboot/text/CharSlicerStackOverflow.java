package io.github.majianzheng.jarboot.text;

import io.github.majianzheng.jarboot.text.util.CharSlicer;
import io.github.majianzheng.jarboot.text.util.Pair;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author wangtao 2017-02-07 11:17.
 */
public class CharSlicerStackOverflow {

    @Test
    public void testNonrecursiveSize() {
        StringBuilder sb = new StringBuilder();
        int lines = 10000;
        int maxLength = 0;
        for (int i = 0; i < lines; i++) {
            int length = (int)(Math.random() * 100);
            if (length > maxLength) maxLength = length;
            for (int j = 0; j < length; j++) {
                sb.append("a");
            }
            sb.append("\n");
        }
        CharSlicer charSlicer = new CharSlicer(sb.toString());
        Assert.assertEquals(charSlicer.size().getFirst().intValue(), maxLength);
        Assert.assertEquals(charSlicer.size().getSecond().intValue(), lines + 1);
    }

    @Test
    public void testLines() {
        StringBuilder sb = new StringBuilder();
        int lines = 10000;
        int maxLength = 0;
        for (int i = 0; i < lines; i++) {
            int length = (int)(Math.random() * 100);
            if (length > maxLength) maxLength = length;
            for (int j = 0; j < length; j++) {
                sb.append("a");
            }
            sb.append("\n");
        }

        CharSlicer charSlicer = new CharSlicer(sb.toString());
        Pair<Integer, Integer>[] strLines = charSlicer.lines(maxLength);
        Assert.assertEquals(lines, strLines.length);
        Assert.assertNotNull(strLines[strLines.length-1]);
    }

    @Test
    public void testSimpleString() {
        CharSlicer charSlicer = new CharSlicer("abcdefghigk");
        Assert.assertEquals(charSlicer.size().getFirst().intValue(), "abcdefghigk".length());
        Assert.assertEquals(charSlicer.size().getSecond().intValue(), 1);
    }

    @Test
    public void testRecursiveVersion() {
        Pair<Integer, Integer> pair = size("abcdefghigk", 0, 1);
        Assert.assertEquals(pair.getFirst().intValue(), "abcdefghigk".length());
        Assert.assertEquals(pair.getSecond().intValue(), 1);
    }

    private static Pair<Integer, Integer> size(String s, int index, int height) {
        if (height < 1) {
            throw new IllegalArgumentException("A non positive height=" + height + " cannot be accepted");
        }
        if (index < s.length()) {
            int pos = s.indexOf('\n', index);
            if (pos == -1) {
                return Pair.of(s.length() - index, height);
            } else {
                Pair<Integer, Integer> ret = size(s, pos + 1, height + 1);
                return new Pair<Integer, Integer>(Math.max(pos - index, ret.getFirst()), ret.getSecond());
            }
        } else {
            return Pair.of(0, height);
        }
    }
}
