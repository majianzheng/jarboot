package com.mz.jarboot.common;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class NetworkUtilsTest {
    @Test
    public void testHostReachable() {
        Assert.assertTrue(NetworkUtils.hostReachable("127.0.0.1"));

        int port = NetworkUtils.findAvailableTcpPort();
        Assert.assertTrue(port > 0);
        Assert.assertTrue(NetworkUtils.isTcpPortAvailable(port));
    }

    @Test
    public void testHostLocal() {
        List<String> list = NetworkUtils.getLocalAddr();
        if (list.isEmpty()) {
            return;
        }
        boolean isLocal = NetworkUtils.hostLocal(list.get(0));
        Assert.assertTrue(isLocal);
        try {
            NetworkUtils.hostLocal("127.0.0.1");
            Assert.fail("不能是环路地址");
        } catch (Exception e) {
            Assert.assertEquals("请输入真实IP地址或域名，而不是环路地址：127.0.0.1", e.getMessage());
        }

        try {
            NetworkUtils.hostLocal("localhost");
            Assert.fail("不能是环路地址");
        } catch (Exception e) {
            Assert.assertEquals("请输入真实IP地址或域名，而不是环路地址：localhost", e.getMessage());
        }
    }

    @Test
    public void testGetLocalAddr() {
        List<String> list = NetworkUtils.getLocalAddr();
        Assert.assertTrue(!list.isEmpty());
    }
}
