package com.mz.jarboot.common;

import org.junit.Assert;
import org.junit.Test;

public class NetworkUtilsTest {
    @Test
    public void testHostReachable() {
        Assert.assertTrue(NetworkUtils.hostReachable("127.0.0.1"));

        int port = NetworkUtils.findAvailableTcpPort();
        Assert.assertTrue(NetworkUtils.isTcpPortAvailable(port));
    }
}
