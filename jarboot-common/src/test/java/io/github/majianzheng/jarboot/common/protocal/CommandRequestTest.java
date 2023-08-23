package io.github.majianzheng.jarboot.common.protocal;

import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.protocol.CommandRequest;
import io.github.majianzheng.jarboot.common.protocol.CommandType;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class CommandRequestTest {

    @Test
    public void testFromRaw() throws IOException {
        //用户命令协议测试
        CommandRequest request = new CommandRequest();
        byte[] raw = toByte(CommandType.USER_PUBLIC.value(), "123\r80,90\rtrace com.demo.Test add");
        request.fromRaw(raw);
        assertEquals(CommandType.USER_PUBLIC, request.getCommandType());
        assertEquals("123", request.getSessionId());
        assertEquals("trace com.demo.Test add", request.getCommandLine());

        //内部命令协议测试
        request = new CommandRequest();
        raw = toByte(CommandType.INTERNAL.value(), "1234\r80,80\rcancel watch");
        request.fromRaw(raw);
        assertEquals(CommandType.INTERNAL, request.getCommandType());
        assertEquals("1234", request.getSessionId());
        assertEquals("cancel watch", request.getCommandLine());

        //异常命令协议测试
        request = new CommandRequest();
        raw = toByte(null, "x1234\r80,80\rcancel watch");
        request.fromRaw(raw);
        assertEquals(CommandType.UNKNOWN, request.getCommandType());
        assertEquals("1234", request.getSessionId());
        assertEquals("cancel watch", request.getCommandLine());

        try {
            request = new CommandRequest();
            raw = toByte(null, "x1234watch");
            request.fromRaw(raw);
            org.junit.Assert.fail("应该抛出协议错误移除");
        } catch (Throwable e) {
            assertTrue(e instanceof JarbootException);
        }
    }

    @Test
    public void testToRaw() throws IOException {
        //用户命令协议测试
        CommandRequest request = new CommandRequest();
        request.setCommandType(CommandType.USER_PUBLIC);
        request.setSessionId("123");
        request.setCol(80);
        request.setRow(80);
        request.setCommandLine("trace com.demo.Test add");

        assertArrayEquals(toByte(CommandType.USER_PUBLIC.value(), "123\r80,80\rtrace com.demo.Test add"), request.toRaw());

        //内部命令协议测试
        request = new CommandRequest();
        request.setCommandType(CommandType.INTERNAL);
        request.setSessionId("1234");
        request.setCol(80);
        request.setRow(80);
        request.setCommandLine("cancel watch");
        assertArrayEquals(toByte(CommandType.INTERNAL.value(), "1234\r80,80\rcancel watch"), request.toRaw());

        //异常命令协议测试
        request = new CommandRequest();
        request.setCommandType(CommandType.UNKNOWN);
        request.setSessionId("1234");
        request.setCol(80);
        request.setRow(80);
        request.setCommandLine("cancel watch");
        assertArrayEquals(toByte(CommandType.UNKNOWN.value(), "1234\r80,80\rcancel watch"), request.toRaw());
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
