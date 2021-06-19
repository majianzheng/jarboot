package com.mz.jarboot.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class VMUtilsTest {
    @Test
    public void test() {
        Assert.assertTrue(VMUtils.getInstance().isInitialized());
        Map<Integer, String> listVm = VMUtils.getInstance().listVM();
        Assert.assertTrue(!listVm.isEmpty());
    }
}
