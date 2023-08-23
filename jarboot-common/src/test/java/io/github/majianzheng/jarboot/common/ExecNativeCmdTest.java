package io.github.majianzheng.jarboot.common;

import io.github.majianzheng.jarboot.common.utils.OSUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author majianzheng
 */
public class ExecNativeCmdTest {
    private static String cmd = "";
    private static String[] cmdArray;
    @BeforeClass
    public static void beforeTest() {
        if (OSUtils.isWindows()) {
            cmd = "cmd /c ";
            cmdArray = new String[]{"cmd", "/c", "echo", "Hi"};
        } else {
            cmdArray = new String[]{"echo", "Hi"};
        }
        cmd += "echo Hello";
    }

    @Test
    public void testExec() {
        List<String> result = ExecNativeCmd.exec(cmd);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Hello", result.get(0));
        result = ExecNativeCmd.exec(cmdArray);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Hi", result.get(0));
    }

    @Test
    public void testGetFirstAnswer() {
        String result = ExecNativeCmd.getFirstAnswer(cmd);
        assertFalse(result.isEmpty());
        assertEquals("Hello", result);
    }

    @Test
    public void testGetAnswerAt() {
        String result = ExecNativeCmd.getAnswerAt(cmd, 0);
        assertFalse(result.isEmpty());
        assertEquals("Hello", result);
    }
}
