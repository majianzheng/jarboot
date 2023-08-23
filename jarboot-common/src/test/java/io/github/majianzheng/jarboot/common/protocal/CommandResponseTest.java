package io.github.majianzheng.jarboot.common.protocal;

import io.github.majianzheng.jarboot.common.protocol.CommandConst;
import io.github.majianzheng.jarboot.common.protocol.CommandResponse;
import io.github.majianzheng.jarboot.common.protocol.ResponseType;
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
        byte header = (byte) (ResponseType.NOTIFY.value() | CommandConst.SUCCESS_FLAG);
        response.fromRaw(toByte(header, "body data\r123"));
        assertEquals(ResponseType.NOTIFY, response.getResponseType());
        assertEquals("123", response.getSessionId());
        assertEquals("body data", response.getBody());
        assertTrue(response.getSuccess());

        response = new CommandResponse();
        header = ResponseType.NOTIFY.value();
        response.fromRaw(toByte(header, "body data\r123"));
        assertEquals(ResponseType.NOTIFY, response.getResponseType());
        assertEquals("123", response.getSessionId());
        assertEquals("body data", response.getBody());
        assertFalse(response.getSuccess());

        response = new CommandResponse();
        header = (byte) (ResponseType.STD_PRINT.value() | CommandConst.SUCCESS_FLAG);
        response.fromRaw(toByte(header, "body data\r123"));
        assertEquals(ResponseType.STD_PRINT, response.getResponseType());
        assertEquals("123", response.getSessionId());
        assertEquals("body data", response.getBody());
        assertTrue(response.getSuccess());

        response = new CommandResponse();
        header = ResponseType.LOG_APPENDER.value();
        response.fromRaw(toByte(header, "body xxx data\r125663"));
        assertEquals(ResponseType.LOG_APPENDER, response.getResponseType());
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
        byte header = (byte) (ResponseType.NOTIFY.value() | CommandConst.SUCCESS_FLAG);
        CommandResponse response = new CommandResponse();
        response.setResponseType(ResponseType.NOTIFY);
        response.setSessionId("123");
        response.setBody("body data");
        response.setSuccess(true);
        assertArrayEquals(toByte(header, "body data\r123"), response.toRaw());

        response = new CommandResponse();
        header = ResponseType.NOTIFY.value();
        response.setResponseType(ResponseType.NOTIFY);
        response.setSessionId("123");
        response.setBody("body data");
        response.setSuccess(false);
        assertArrayEquals(toByte(header, "body data\r123"), response.toRaw());

        response = new CommandResponse();
        header = (byte) (ResponseType.STD_PRINT.value() | CommandConst.SUCCESS_FLAG);
        response.setResponseType(ResponseType.STD_PRINT);
        response.setSessionId("123");
        response.setBody("body data");
        response.setSuccess(true);
        assertArrayEquals(toByte(header, "body data\r123"), response.toRaw());

        response = new CommandResponse();
        header = ResponseType.LOG_APPENDER.value();
        response.setResponseType(ResponseType.LOG_APPENDER);
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
