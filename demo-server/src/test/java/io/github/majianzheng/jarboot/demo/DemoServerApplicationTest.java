package io.github.majianzheng.jarboot.demo;

import org.junit.Assert;
import org.junit.Test;

public class DemoServerApplicationTest {
    @Test
    public void testFib() {
        int r = DemoServerApplication.fib(3);
        DemoServerApplication.log("result: " + r);
        Assert.assertEquals(2, r);

        r = DemoServerApplication.fib(10);
        DemoServerApplication.log("result: " + r);
        Assert.assertEquals(55, r);
    }

    @Test
    public void testPow() {
        double r = DemoServerApplication.pow(2, 4);
        DemoServerApplication.log("result: " + r);
        Assert.assertEquals("16.0", "" + r);

        r = DemoServerApplication.pow(3, 3);
        DemoServerApplication.log("result: " + r);
        Assert.assertEquals("27.0", "" + r);
    }
}
