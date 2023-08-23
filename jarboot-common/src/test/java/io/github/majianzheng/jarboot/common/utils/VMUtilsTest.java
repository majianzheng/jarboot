package io.github.majianzheng.jarboot.common.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * @author majianzheng
 */
public class VMUtilsTest {
    @Test
    public void test() {
        Map<String, String> listVm = VMUtils.getInstance().listVM();
        Assert.assertFalse(listVm.isEmpty());
    }
}
