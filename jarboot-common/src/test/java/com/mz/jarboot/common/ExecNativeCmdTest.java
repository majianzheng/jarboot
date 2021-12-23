package com.mz.jarboot.common;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author majianzheng
 */
public class ExecNativeCmdTest {

    @Test
    public void testExec() {
        List<String> result = ExecNativeCmd.exec("echo Hello");
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Hello", result.get(0));
        result = ExecNativeCmd.exec(new String[]{"echo", "Hi"});
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Hi", result.get(0));
    }

    @Test
    public void testGetFirstAnswer() {
        String result = ExecNativeCmd.getFirstAnswer("echo Hello");
        assertFalse(result.isEmpty());
        assertEquals("Hello", result);
    }

    @Test
    public void testGetAnswerAt() {
        String result = ExecNativeCmd.getAnswerAt("echo Hello", 0);
        assertFalse(result.isEmpty());
        assertEquals("Hello", result);
    }
}
