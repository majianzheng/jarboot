package com.mz.jarboot.core.cmd.impl;

import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.cmd.Command;
import com.mz.jarboot.core.session.CommandSession;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.utils.StringUtils;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * show the jvm detail
 * @author jianzhengma
 */
public class BytesCommandImpl extends Command {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private static Printer printer = new Textifier();
    private static TraceMethodVisitor mp = new TraceMethodVisitor(printer);
    private CommandSession handler = null;

    @Override
    public boolean isRunning() {
        return null != handler && handler.isRunning();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void cancel() {
        //do nothing
    }

    @Override
    public void run(CommandSession handler) {
        this.handler = handler;

        logger.info("bytes 开始执行>>{}", name);
        if (StringUtils.isEmpty(this.args)) {
            //未指定要打印的类
            handler.console("用法: bytes className");
            complete();
            return;
        }
        Class<?> cls;
        try {
            cls = Class.forName(this.args);
        } catch (ClassNotFoundException e) {
            handler.console("没有找到类," + this.args);
            complete();
            return;
        }
        EnvironmentContext.getTransformerManager()
                .addOnceTransformer(cls, (loader, className, classBeingRedefined,
                                          protectionDomain, classfileBuffer) -> {

                    logger.info("Bytes. >>{}", className);

                    try {
                        ClassReader reader = new ClassReader(classfileBuffer);
                        ClassNode classNode = new ClassNode();
                        reader.accept(classNode, 0);
                        final List<MethodNode> methods = classNode.methods;
                        for (MethodNode m : methods) {
                            InsnList inList = m.instructions;
                            handler.console(m.name);
                            for (int i = 0; i < inList.size(); i++) {
                                handler.console(nodeToString(inList.get(i)));
                            }
                        }
                    } catch (Exception e) {
                        logger.warn(e.getMessage(), e);
                        handler.console("解析类失败，" + e.getMessage());
                    }
                    complete();
                    return null;
                });

        EnvironmentContext.getTransformerManager().retransformClasses(cls);
    }

    public static String nodeToString(AbstractInsnNode node){
        node.accept(mp);
        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();
        return sw.toString();
    }

    @Override
    public void complete() {
        if (null != handler) {
            handler.end();
        }
    }
}
