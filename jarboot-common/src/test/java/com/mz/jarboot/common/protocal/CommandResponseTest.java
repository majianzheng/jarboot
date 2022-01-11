package com.mz.jarboot.common.protocal;

import com.mz.jarboot.common.protocol.CommandConst;
import com.mz.jarboot.common.protocol.CommandResponse;
import com.mz.jarboot.common.protocol.ResponseType;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class CommandResponseTest {
    @Test
    public void testFromRaw() throws IOException {
        CommandResponse response = new CommandResponse();
        byte header = (byte) (ResponseType.CONSOLE.value() | CommandConst.SUCCESS_FLAG);
        response.fromRaw(toByte(header, "body data\r123"));
        assertEquals(ResponseType.CONSOLE, response.getResponseType());
        assertEquals("123", response.getSessionId());
        assertEquals("body data", response.getBody());
        assertTrue(response.getSuccess());

        response = new CommandResponse();
        header = ResponseType.CONSOLE.value();
        response.fromRaw(toByte(header, "body data\r123"));
        assertEquals(ResponseType.CONSOLE, response.getResponseType());
        assertEquals("123", response.getSessionId());
        assertEquals("body data", response.getBody());
        assertFalse(response.getSuccess());

        response = new CommandResponse();
        header = (byte) (ResponseType.JSON_RESULT.value() | CommandConst.SUCCESS_FLAG);
        response.fromRaw(toByte(header, "body data\r123"));
        assertEquals(ResponseType.JSON_RESULT, response.getResponseType());
        assertEquals("123", response.getSessionId());
        assertEquals("body data", response.getBody());
        assertTrue(response.getSuccess());

        response = new CommandResponse();
        header = ResponseType.COMMAND_END.value();
        response.fromRaw(toByte(header, "body xxx data\r125663"));
        assertEquals(ResponseType.COMMAND_END, response.getResponseType());
        assertEquals("125663", response.getSessionId());
        assertEquals("body xxx data", response.getBody());
        assertFalse(response.getSuccess());

        //协议错误
        response = new CommandResponse();
        response.fromRaw(toByte(null, "x1234watch"));
        assertFalse(response.getSuccess());
    }

    @Test
    public void testToRaw() throws IOException {
        byte header = (byte) (ResponseType.CONSOLE.value() | CommandConst.SUCCESS_FLAG);
        CommandResponse response = new CommandResponse();
        response.setResponseType(ResponseType.CONSOLE);
        response.setSessionId("123");
        response.setBody("body data");
        response.setSuccess(true);
        assertArrayEquals(toByte(header, "body data\r123"), response.toRaw());

        response = new CommandResponse();
        header = ResponseType.CONSOLE.value();
        response.setResponseType(ResponseType.CONSOLE);
        response.setSessionId("123");
        response.setBody("body data");
        response.setSuccess(false);
        assertArrayEquals(toByte(header, "body data\r123"), response.toRaw());

        response = new CommandResponse();
        header = (byte) (ResponseType.JSON_RESULT.value() | CommandConst.SUCCESS_FLAG);
        response.setResponseType(ResponseType.JSON_RESULT);
        response.setSessionId("123");
        response.setBody("body data");
        response.setSuccess(true);
        assertArrayEquals(toByte(header, "body data\r123"), response.toRaw());

        response = new CommandResponse();
        header = ResponseType.COMMAND_END.value();
        response.setResponseType(ResponseType.COMMAND_END);
        response.setSessionId("125663");
        response.setBody("body xxx data");
        response.setSuccess(false);
        assertArrayEquals(toByte(header, "body xxx data\r125663"), response.toRaw());
    }

    private byte[] toByte(Byte type, String cmd) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if (null != type) {
            byteArrayOutputStream.write(type);
        }
        byteArrayOutputStream.write(cmd.getBytes(StandardCharsets.UTF_8));
        return byteArrayOutputStream.toByteArray();
    }
}
