package io.github.majianzheng.jarboot.core.utils.matcher;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author majianzheng
 */
public class TrueMatcherTest {

    @Test
    public void testMatching(){
        Assert.assertTrue(new TrueMatcher<String>().matching(null));
        Assert.assertTrue(new TrueMatcher<Integer>().matching(1));
        Assert.assertTrue(new TrueMatcher<String>().matching(""));
        Assert.assertTrue(new TrueMatcher<String>().matching("foobar"));
    }

}
