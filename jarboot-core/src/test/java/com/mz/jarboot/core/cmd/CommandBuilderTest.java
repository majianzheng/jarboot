package com.mz.jarboot.core.cmd;

import com.mz.jarboot.common.CommandRequest;
import com.mz.jarboot.core.cmd.impl.ThreadCommand;
import com.mz.jarboot.core.cmd.impl.TraceCommandImpl;
import com.mz.jarboot.core.server.LogTest;
import com.mz.jarboot.core.session.CommandSession;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("all")
public class CommandBuilderTest {
    @BeforeClass
    public static void init() {
        LogTest.initTest();
    }
    @Test
    public void testBuild() {
        //测试trace命令构建
        String line = "u123 trace demo.Test run 'params.length>=0' -n 5";
        CommandRequest request = new CommandRequest();
        CommandSession session = Mockito.mock(CommandSession.class);
        request.fromRaw(line);
        Command cmd = CommandBuilder.build(request, session);
        assertThat(cmd instanceof TraceCommandImpl).isTrue();
        TraceCommandImpl trace = (TraceCommandImpl)cmd;
        assertEquals("trace", trace.getName());
        assertEquals("demo.Test", trace.getClassPattern());
        assertEquals("run", trace.getMethodPattern());
        assertEquals(5, trace.getNumberOfLimit());
        assertEquals("'params.length>=0'", trace.getConditionExpress());

        //测试thread命令构建
        line = "u123 thread 1";
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
    }
}
