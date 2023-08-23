package io.github.majianzheng.jarboot.core.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author majianzheng
 */
public class JarbootCheckUtilsTest {

    @Test
    public void testIsIn(){
        Assert.assertTrue(JarbootCheckUtils.isIn(1,1,2,3));
        Assert.assertFalse(JarbootCheckUtils.isIn(1,2,3,4));
        Assert.assertTrue(JarbootCheckUtils.isIn(null,1,null,2));
        Assert.assertFalse(JarbootCheckUtils.isIn(1,null));
        Assert.assertTrue(JarbootCheckUtils.isIn(1L,1L,2L,3L));
        Assert.assertFalse(JarbootCheckUtils.isIn(1L,2L,3L,4L));
        Assert.assertTrue(JarbootCheckUtils.isIn("foo","foo","bar"));
        Assert.assertFalse(JarbootCheckUtils.isIn("foo","bar","goo"));
    }


    @Test
    public void testIsEquals(){
        Assert.assertTrue(JarbootCheckUtils.isEquals(1,1));
        Assert.assertTrue(JarbootCheckUtils.isEquals(1L,1L));
        Assert.assertTrue(JarbootCheckUtils.isEquals("foo","foo"));
        Assert.assertFalse(JarbootCheckUtils.isEquals(1,2));
        Assert.assertFalse(JarbootCheckUtils.isEquals("foo","bar"));
    }
}
