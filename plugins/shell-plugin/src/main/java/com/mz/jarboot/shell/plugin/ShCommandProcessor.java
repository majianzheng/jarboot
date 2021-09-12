package com.mz.jarboot.shell.plugin;

import com.mz.jarboot.api.JarbootFactory;
import com.mz.jarboot.api.cmd.annotation.Description;
import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.annotation.Summary;
import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.api.cmd.spi.CommandProcessor;
import java.lang.reflect.Method;
import java.util.List;

/**
 * sh
 * @author majianzheng
 */
@Name("sh")
@Summary("Execute shell")
@Description(" sh xxx.sh")
public class ShCommandProcessor implements CommandProcessor {
    private static final Method EXEC_METHOD;
    static {
        ClassLoader classLoader = JarbootFactory.createAgentService().getJarbootClassLoader();
        Method temp = null;
        try {
            Class<?> execClass = classLoader.loadClass("com.mz.jarboot.common.ExecNativeCmd");
            temp = execClass.getMethod("exec", String.class);
        } catch (Exception e) {
            //ignore
        }
        EXEC_METHOD = temp;
    }

    @Override
    public String process(CommandSession session, String[] args) {
        if (null == EXEC_METHOD) {
            session.end(false, "Load exec method failed!");
            return "";
        }
        if (null != args && args.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (String s : args) {
                sb.append(s).append(' ');
            }
            try {
                Object result = EXEC_METHOD.invoke(null, sb.toString());
                if (result instanceof List) {
                    List<?> list = (List<?>) result;
                    list.forEach(line -> session.console((String)line));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            session.end(false, "args is empty!");
        }
        return "";
    }
}
