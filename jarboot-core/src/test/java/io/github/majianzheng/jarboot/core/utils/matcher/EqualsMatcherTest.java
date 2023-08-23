package io.github.majianzheng.jarboot.core.utils.matcher;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author majianzheng
 */
public class EqualsMatcherTest {

    @Test
    public void testMatching(){
        Assert.assertTrue(new EqualsMatcher<String>(null).matching(null));
        Assert.assertTrue(new EqualsMatcher<String>("").matching(""));
        Assert.assertTrue(new EqualsMatcher<String>("foobar").matching("foobar"));
        Assert.assertFalse(new EqualsMatcher<String>("").matching(null));
        Assert.assertFalse(new EqualsMatcher<String>("abc").matching("def"));
    }

}
