package io.github.majianzheng.jarboot.core.cmd.impl;

import com.alibaba.bytekit.utils.IOUtils;
import io.github.majianzheng.jarboot.api.cmd.annotation.Name;
import io.github.majianzheng.jarboot.api.cmd.annotation.Summary;
import io.github.majianzheng.jarboot.common.ExecNativeCmd;
import io.github.majianzheng.jarboot.core.basic.EnvironmentContext;
import io.github.majianzheng.jarboot.core.cmd.AbstractCommand;
import io.github.majianzheng.jarboot.api.cmd.annotation.Argument;
import io.github.majianzheng.jarboot.api.cmd.annotation.Description;
import io.github.majianzheng.jarboot.core.constant.CoreConstant;
import io.github.majianzheng.jarboot.core.utils.LogUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * show the class byte detail
 * @author majianzheng
 */
@Name("bytes")
@Summary("Show the class byte detail")
@Description(CoreConstant.EXAMPLE +
        "  bytes java.lang.String\n" +
        CoreConstant.WIKI + CoreConstant.WIKI_HOME + "bytes")
public class BytesCommand extends AbstractCommand {
    private static final Logger logger = LogUtils.getLogger();

    private String classPattern;

    @Argument(argName = "class-pattern", index = 0)
    @Description("Class name pattern, use either '.' or '/' as separator")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void run() {
        if (StringUtils.isEmpty(this.classPattern)) {
            //未指定要打印的类
            session.end(true, "用法: bytes className");
            return;
        }
        String javaHome = System.getenv("JAVA_HOME");
        if (StringUtils.isEmpty(javaHome)) {
            session.console("JAVA_HOME is not set!");
        }
        Class<?> cls = null;
        Class[] classes = EnvironmentContext.getInstrumentation().getAllLoadedClasses();
        for (Class<?> c : classes) {
            if (c.getName().equals(this.classPattern)) {
                cls = c;
                break;
            }
        }
        if (null == cls) {
            session.end(true, "Not find," + this.classPattern);
            return;
        }
        //打印classloader
        session.console("ClassLoader: " + cls.getClassLoader().toString());
        session.console("\n");
        showBytesCode(cls);
    }

    private synchronized void showBytesCode(Class<?> cls) {
        File file = null;
        try {
            byte[] classfileBuffer = IOUtils.getBytes(Objects.requireNonNull(cls.getClassLoader()
                    .getResourceAsStream(cls.getName().replace('.', '/') + ".class")));
            StringBuilder sb = new StringBuilder();
            sb
                    .append(LogUtils.getLogDir())
                    .append(File.separator)
                    .append(CoreConstant.DUMP_DIR);
            File dir = new File(sb.toString());
            if (!dir.exists()) {
                FileUtils.forceMkdir(dir);
            }
            file = new File(dir, cls.getSimpleName() + ".class");
            FileUtils.writeByteArrayToFile(file, classfileBuffer, false);
            List<String> codes = ExecNativeCmd.exec("javap -v -c " + file.getAbsolutePath());
            codes.forEach(l -> session.console(l));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (null != file) {
                try {
                    FileUtils.delete(file);
                } catch (Exception e) {
                    //ignore
                }
            }
            session.end();
        }
    }
}
