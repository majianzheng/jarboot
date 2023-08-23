package io.github.majianzheng.jarboot.common.utils;

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
        NetworkUtils.hostLocal("127.0.0.1");
        Assert.assertTrue(NetworkUtils.hostLocal("127.0.0.1"));
        Assert.assertTrue(NetworkUtils.hostLocal("localhost"));
    }

    @Test
    public void testGetLocalAddr() {
        List<String> list = NetworkUtils.getLocalAddr();
        Assert.assertFalse(list.isEmpty());
    }
}
