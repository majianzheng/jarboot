package com.mz.jarboot.common.protocal;

import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.common.protocol.CommandRequest;
import com.mz.jarboot.common.protocol.CommandType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommandRequestTest {

    @Test
    public void testFromRaw() {
        //用户命令协议测试
        CommandRequest request = new CommandRequest();
        request.fromRaw(CommandType.USER_PUBLIC.value() +  "123\rtrace com.demo.Test add");
        assertEquals(CommandType.USER_PUBLIC, request.getCommandType());
        assertEquals("123", request.getSessionId());
        assertEquals("trace com.demo.Test add", request.getCommandLine());

        //内部命令协议测试
        request = new CommandRequest();
        request.fromRaw(CommandType.INTERNAL.value() + "1234\rcancel watch");
        assertEquals(CommandType.INTERNAL, request.getCommandType());
        assertEquals("1234", request.getSessionId());
        assertEquals("cancel watch", request.getCommandLine());

        //异常命令协议测试
        request = new CommandRequest();
        request.fromRaw("x1234\rcancel watch");
        assertEquals(CommandType.UNKNOWN, request.getCommandType());
        assertEquals("1234", request.getSessionId());
        assertEquals("cancel watch", request.getCommandLine());

        try {
            request = new CommandRequest();
            request.fromRaw("x1234watch");
            org.junit.Assert.fail("应该抛出协议错误移除");
        } catch (Throwable e) {
            assertTrue(e instanceof JarbootException);
        }
    }

    @Test
    public void testToRaw() {
        //用户命令协议测试
        CommandRequest request = new CommandRequest();
        request.setCommandType(CommandType.USER_PUBLIC);
        request.setSessionId("123");
        request.setCommandLine("trace com.demo.Test add");
        assertEquals(CommandType.USER_PUBLIC.value() +  "123\rtrace com.demo.Test add", request.toRaw());

        //内部命令协议测试
        request = new CommandRequest();
        request.setCommandType(CommandType.INTERNAL);
        request.setSessionId("1234");
        request.setCommandLine("cancel watch");
        assertEquals(CommandType.INTERNAL.value() + "1234\rcancel watch", request.toRaw());

        //异常命令协议测试
        request = new CommandRequest();
        request.setCommandType(CommandType.UNKNOWN);
        request.setSessionId("1234");
        request.setCommandLine("cancel watch");
        assertEquals(CommandType.UNKNOWN.value() + "1234\rcancel watch", request.toRaw());
    }
}
