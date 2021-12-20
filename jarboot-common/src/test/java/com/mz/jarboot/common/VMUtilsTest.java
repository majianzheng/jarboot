package com.mz.jarboot.common;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * @author majianzheng
 */
public class VMUtilsTest {
    @Test
    public void test() {
        Map<Integer, String> listVm = VMUtils.getInstance().listVM();
        Assert.assertFalse(listVm.isEmpty());
    }
}
