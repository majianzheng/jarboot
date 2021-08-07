package com.mz.jarboot.core.cmd.impl;

import com.alibaba.bytekit.utils.IOUtils;
import com.alibaba.deps.org.objectweb.asm.ClassReader;
import com.alibaba.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import com.alibaba.deps.org.objectweb.asm.util.Printer;
import com.alibaba.deps.org.objectweb.asm.util.Textifier;
import com.alibaba.deps.org.objectweb.asm.util.TraceMethodVisitor;
import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.annotation.Summary;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.cmd.AbstractCommand;
import com.mz.jarboot.api.cmd.annotation.Argument;
import com.mz.jarboot.api.cmd.annotation.Description;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.PrintWriter;
import java.io.StringWriter;
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
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private static Printer printer = new Textifier();
    private static TraceMethodVisitor mp = new TraceMethodVisitor(printer);

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
        logger.info("bytes 开始执行>>{}", name);
        if (StringUtils.isEmpty(this.classPattern)) {
            //未指定要打印的类
            session.end(true, "用法: bytes className");
            return;
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
        session.console("<hr>");
        try {
            byte[] classfileBuffer = IOUtils.getBytes(Objects.requireNonNull(cls.getClassLoader()
                    .getResourceAsStream(cls.getName().replace('.', '/') + ".class")));
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            final List<MethodNode> methods = classNode.methods;
            for (MethodNode m : methods) {
                InsnList inList = m.instructions;
                session.console(m.name);
                for (int i = 0; i < inList.size(); i++) {
                    session.console(nodeToString(inList.get(i)));
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            session.end();
        }
    }

    public static String nodeToString(AbstractInsnNode node){
        node.accept(mp);
        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();
        return sw.toString();
    }
}
