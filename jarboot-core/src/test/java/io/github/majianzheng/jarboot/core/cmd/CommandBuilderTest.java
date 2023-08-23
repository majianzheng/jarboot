package io.github.majianzheng.jarboot.core.cmd;

import io.github.majianzheng.jarboot.common.protocol.CommandRequest;
import io.github.majianzheng.jarboot.common.protocol.CommandType;
import io.github.majianzheng.jarboot.core.cmd.impl.SearchClassCommand;
import io.github.majianzheng.jarboot.core.cmd.impl.ThreadCommand;
import io.github.majianzheng.jarboot.core.cmd.impl.TraceCommand;
import io.github.majianzheng.jarboot.core.cmd.internal.CancelCommand;
import io.github.majianzheng.jarboot.core.cmd.internal.ExitCommand;
import io.github.majianzheng.jarboot.core.session.AbstractCommandSession;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author majianzheng
 */
@SuppressWarnings("java:S5838")
public class CommandBuilderTest {
    @Test
    public void testBuild() throws IOException {
        //测试trace命令构建
        byte[] line = toByte(CommandType.USER_PUBLIC.value(),
                "123\r80,100\rtrace demo.Test run 'params.length>=0' -n 5");
        CommandRequest request = new CommandRequest();
        AbstractCommandSession session = Mockito.mock(AbstractCommandSession.class);
        request.fromRaw(line);
        AbstractCommand cmd = CommandBuilder.build(request, session);
        assertThat(cmd instanceof TraceCommand).isTrue();
        TraceCommand trace = (TraceCommand)cmd;
        assertEquals("trace", trace.getName());
        assertEquals("demo.Test", trace.getClassPattern());
        assertEquals("run", trace.getMethodPattern());
        assertEquals(5, trace.getNumberOfLimit());
        assertEquals("params.length>=0", trace.getConditionExpress());

        line = toByte(CommandType.USER_PUBLIC.value(),
                "123\r40,80\rtrace demo.Test run 'params.length>=0' -n 3 -p path1 path2 path3");
        request = new CommandRequest();
        session = Mockito.mock(AbstractCommandSession.class);
        request.fromRaw(line);
        cmd = CommandBuilder.build(request, session);
        assertThat(cmd instanceof TraceCommand).isTrue();
        trace = (TraceCommand)cmd;
        assertEquals("trace", trace.getName());
        assertEquals("demo.Test", trace.getClassPattern());
        assertEquals("run", trace.getMethodPattern());
        assertEquals(3, trace.getNumberOfLimit());
        assertEquals("params.length>=0", trace.getConditionExpress());
        List<String> patterns = trace.getPathPatterns();
        assertEquals(3, patterns.size());

        //测试thread命令构建
        line = toByte(CommandType.USER_PUBLIC.value(), "123\r10,10\rthread 1");
        request = new CommandRequest();
        request.fromRaw(line);
        cmd = CommandBuilder.build(request, session);
        assertThat(cmd instanceof ThreadCommand).isTrue();
        ThreadCommand thread = (ThreadCommand)cmd;
        assertEquals("thread", thread.getName());
        try {
            Field field = ThreadCommand.class.getDeclaredField("id");
            field.setAccessible(true);
            assertEquals(1L, field.get(thread));
        } catch (Exception e) {
            org.junit.Assert.fail(e.getMessage());
        }

        //测试thread命令构建
        line = toByte(CommandType.USER_PUBLIC.value(), "123\r80,100\rsc -d -f io.github.majianzheng.jarboot.core.ws.WebSocketClient");
        request = new CommandRequest();
        request.fromRaw(line);
        cmd = CommandBuilder.build(request, session);
        assertThat(cmd instanceof SearchClassCommand).isTrue();
        SearchClassCommand sc = (SearchClassCommand)cmd;
        assertEquals("sc", sc.getName());
        try {
            Field field = SearchClassCommand.class.getDeclaredField("classPattern");
            field.setAccessible(true);
            assertEquals("io.github.majianzheng.jarboot.core.ws.WebSocketClient", field.get(sc));

            field = SearchClassCommand.class.getDeclaredField("isDetail");
            field.setAccessible(true);
            assertEquals(true, field.get(sc));

            field = SearchClassCommand.class.getDeclaredField("isField");
            field.setAccessible(true);
            assertEquals(true, field.get(sc));
        } catch (Exception e) {
            org.junit.Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testInternalBuild() throws IOException {
        //测试trace命令构建
        byte[] line = toByte(CommandType.USER_PUBLIC.value(), "123\r2,7\rexit");
        CommandRequest request = new CommandRequest();
        AbstractCommandSession session = Mockito.mock(AbstractCommandSession.class);
        request.fromRaw(line);
        AbstractCommand cmd = CommandBuilder.build(request, session);
        assertThat(cmd instanceof ExitCommand).isTrue();
        assertEquals("exit", cmd.getName());

        line = toByte(CommandType.INTERNAL.value(), "123\r10,60\rcancel ");
        request = new CommandRequest();
        request.fromRaw(line);
        cmd = CommandBuilder.build(request, session);
        assertThat(cmd instanceof CancelCommand).isTrue();
        assertEquals("cancel", cmd.getName());
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
