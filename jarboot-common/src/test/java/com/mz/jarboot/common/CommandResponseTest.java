package com.mz.jarboot.common;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class CommandResponseTest {
    @Test
    public void testFromRaw() {
        CommandResponse response = new CommandResponse();
        response.fromRaw("k1-body data 123");
        assertEquals(ResponseType.ACK, response.getResponseType());
        assertEquals("123", response.getSessionId());
        assertEquals("body data", response.getBody());
        assertTrue(response.getSuccess());

        response = new CommandResponse();
        response.fromRaw("c0-body data 123");
        assertEquals(ResponseType.CONSOLE, response.getResponseType());
        assertEquals("123", response.getSessionId());
        assertEquals("body data", response.getBody());
        assertFalse(response.getSuccess());

        response = new CommandResponse();
        response.fromRaw("j1-body data 123");
        assertEquals(ResponseType.JSON_RESULT, response.getResponseType());
        assertEquals("123", response.getSessionId());
        assertEquals("body data", response.getBody());
        assertTrue(response.getSuccess());

        response = new CommandResponse();
        response.fromRaw("F0-body xxx data 125663");
        assertEquals(ResponseType.COMMAND_END, response.getResponseType());
        assertEquals("125663", response.getSessionId());
        assertEquals("body xxx data", response.getBody());
        assertFalse(response.getSuccess());

        //协议错误
        response = new CommandResponse();
        response.fromRaw("x1234watch");
        assertFalse(response.getSuccess());
    }

    @Test
    public void testToRaw() {
        CommandResponse response = new CommandResponse();
        response.setResponseType(ResponseType.ACK);
        response.setSessionId("123");
        response.setBody("body data");
        response.setSuccess(true);
        assertEquals("k1-body data 123", response.toRaw());

        response = new CommandResponse();
        response.setResponseType(ResponseType.CONSOLE);
        response.setSessionId("123");
        response.setBody("body data");
        response.setSuccess(false);
        assertEquals("c0-body data 123", response.toRaw());

        response = new CommandResponse();
        response.setResponseType(ResponseType.JSON_RESULT);
        response.setSessionId("123");
        response.setBody("body data");
        response.setSuccess(true);
        assertEquals("j1-body data 123", response.toRaw());

        response = new CommandResponse();
        response.setResponseType(ResponseType.COMMAND_END);
        response.setSessionId("125663");
        response.setBody("body xxx data");
        response.setSuccess(false);
        assertEquals("F0-body xxx data 125663", response.toRaw());
    }
}
