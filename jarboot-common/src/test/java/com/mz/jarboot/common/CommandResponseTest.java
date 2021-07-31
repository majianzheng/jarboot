package com.mz.jarboot.common;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class CommandResponseTest {
    @Test
    public void testFromRaw() {
        CommandResponse response = new CommandResponse();
        char header = CommandConst.CONSOLE_TYPE | CommandConst.SUCCESS_FLAG;
        response.fromRaw(header + "body data\r123");
        assertEquals(ResponseType.CONSOLE, response.getResponseType());
        assertEquals("123", response.getSessionId());
        assertEquals("body data", response.getBody());
        assertTrue(response.getSuccess());

        response = new CommandResponse();
        header = CommandConst.CONSOLE_TYPE;
        response.fromRaw(header + "body data\r123");
        assertEquals(ResponseType.CONSOLE, response.getResponseType());
        assertEquals("123", response.getSessionId());
        assertEquals("body data", response.getBody());
        assertFalse(response.getSuccess());

        response = new CommandResponse();
        header = CommandConst.JSON_RESULT_TYPE | CommandConst.SUCCESS_FLAG;
        response.fromRaw(header + "body data\r123");
        assertEquals(ResponseType.JSON_RESULT, response.getResponseType());
        assertEquals("123", response.getSessionId());
        assertEquals("body data", response.getBody());
        assertTrue(response.getSuccess());

        response = new CommandResponse();
        header = CommandConst.CMD_END_TYPE;
        response.fromRaw(header + "body xxx data\r125663");
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
        char header = CommandConst.CONSOLE_TYPE | CommandConst.SUCCESS_FLAG;
        CommandResponse response = new CommandResponse();
        response.setResponseType(ResponseType.CONSOLE);
        response.setSessionId("123");
        response.setBody("body data");
        response.setSuccess(true);
        assertEquals(header + "body data\r123", response.toRaw());

        response = new CommandResponse();
        header = CommandConst.CONSOLE_TYPE;
        response.setResponseType(ResponseType.CONSOLE);
        response.setSessionId("123");
        response.setBody("body data");
        response.setSuccess(false);
        assertEquals(header + "body data\r123", response.toRaw());

        response = new CommandResponse();
        header = CommandConst.JSON_RESULT_TYPE | CommandConst.SUCCESS_FLAG;
        response.setResponseType(ResponseType.JSON_RESULT);
        response.setSessionId("123");
        response.setBody("body data");
        response.setSuccess(true);
        assertEquals(header + "body data\r123", response.toRaw());

        response = new CommandResponse();
        header = CommandConst.CMD_END_TYPE;
        response.setResponseType(ResponseType.COMMAND_END);
        response.setSessionId("125663");
        response.setBody("body xxx data");
        response.setSuccess(false);
        assertEquals(header + "body xxx data\r125663", response.toRaw());
    }
}
