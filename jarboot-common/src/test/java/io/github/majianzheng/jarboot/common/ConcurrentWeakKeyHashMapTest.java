package io.github.majianzheng.jarboot.common;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;

/**
 * @author majianzheng
 */
public class ConcurrentWeakKeyHashMapTest {

    @Test
    public void testPutGet() {
        ConcurrentWeakKeyHashMap<String, String> map = new ConcurrentWeakKeyHashMap<>();
        assertTrue(map.isEmpty());
        map.put("1", "a");
        map.put("2", "b");
        assertFalse(map.isEmpty());
        assertEquals("a", map.get("1"));
        assertEquals("b", map.get("2"));
        assertNotEquals("b", map.get("1"));
        assertNull(map.get("3"));
        final String defaultValue = "default value";
        assertEquals(defaultValue, map.getOrDefault("not exist", defaultValue));
        for (int i = 0; i < 64; ++i) {
            map.put("" + i, "" + i);
        }
        assertEquals("1", map.get("1"));
        assertEquals("2", map.get("2"));
        assertNotEquals("5", map.get("1"));
        assertNotNull(map.get("3"));
    }

    @Test
    public void testContains() {
        ConcurrentWeakKeyHashMap<String, String> map = new ConcurrentWeakKeyHashMap<>(16);
        map.put("1", "a");
        map.put("2", "b");
        map.put("4", "c");
        map.put("6", "b");
        assertFalse(map.isEmpty());
        assertTrue(map.containsKey("1"));
        assertTrue(map.containsKey("2"));
        assertFalse(map.containsKey("3"));
        assertTrue(map.containsValue("a"));
        assertTrue(map.containsValue("b"));
        assertTrue(map.contains("a"));
        assertTrue(map.contains("b"));
    }

    @Test
    public void testRemove() {
        ConcurrentWeakKeyHashMap<String, String> map = new ConcurrentWeakKeyHashMap<>(64);
        assertTrue(map.isEmpty());
        for (int i = 0; i < 64; ++i) {
            map.put("" + i, "" + i);
        }
        assertEquals(64, map.size());

        assertTrue(map.containsKey("63"));
        map.remove("63");
        assertEquals(63, map.size());
        assertFalse(map.containsKey("63"));
        assertFalse(map.remove("63", "3"));
        assertEquals(63, map.size());

        assertTrue(map.containsKey("30"));
        map.remove("30");
        assertEquals(62, map.size());
        assertFalse(map.containsKey("30"));
        assertFalse(map.remove("30", "3"));
        assertEquals(62, map.size());

        assertTrue(map.containsKey("0"));
        map.remove("0");
        assertEquals(61, map.size());
        assertFalse(map.containsKey("0"));
        assertFalse(map.remove("0", "3"));
        assertEquals(61, map.size());

        assertEquals("1", map.replace("1", "a"));
        assertEquals("a", map.get("1"));

        assertFalse(map.replace("1", "1", "c"));
        assertTrue(map.replace("1", "a", "c"));
        assertEquals("c", map.get("1"));

        map.clear();
        assertEquals(0, map.size());
    }
}
