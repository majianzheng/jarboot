package com.mz.jarboot.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.mz.jarboot.common.AgentClientPojo;
import com.mz.jarboot.common.AnsiLog;
import org.junit.Test;

import static org.junit.Assert.*;

public class JsonUtilsTest {

    @Test
    public void testReadAsJsonNode() {
        JsonNode json = JsonUtils.readAsJsonNode("{\"a\":1, \"b\": \"b\", \"c\":true}");
        assertNotNull(json);
        assertEquals(1, json.get("a").asInt());
        assertEquals("b", json.get("b").asText());
        assertTrue(json.get("c").asBoolean());

        assertNull(JsonUtils.readAsJsonNode("\"{xxx"));
    }

    @Test
    public void testReadValue() {
        String content = "{\"resultCode\":-1,\"resultMsg\":null,\"total\":null,\"clientAddr\":\"192.168.1.100\"," +
                "\"local\":null,\"serviceName\":null,\"sid\":\"test-sid\"," +
                "\"host\":\"192.168.1.101:9899\",\"diagnose\":true}";
        AgentClientPojo obj = JsonUtils.readValue(content, AgentClientPojo.class);
        assertNotNull(obj);
        assertEquals("192.168.1.101:9899", obj.getHost());
        assertEquals("test-sid", obj.getSid());
        assertEquals(true, obj.getDiagnose());
        assertEquals(-1, obj.getResultCode());
    }

    @Test
    public void testToJsonString() {
        AgentClientPojo obj = new AgentClientPojo();
        obj.setDiagnose(true);
        obj.setHost("192.168.1.101:9899");
        obj.setClientAddr("192.168.1.100");
        obj.setSid("test-sid");
        obj.setResultCode(-1);
        String content = JsonUtils.toJsonString(obj);
        assertNotNull(content);
        assertFalse(content.isEmpty());
        AnsiLog.info(content);
        obj = JsonUtils.readValue(content, AgentClientPojo.class);
        assertNotNull(obj);
        assertEquals("192.168.1.100", obj.getClientAddr());
    }
}
