package io.github.majianzheng.jarboot.core.cmd.impl;

import io.github.majianzheng.jarboot.api.cmd.annotation.*;
import io.github.majianzheng.jarboot.core.basic.EnvironmentContext;
import io.github.majianzheng.jarboot.core.cmd.AbstractCommand;
import io.github.majianzheng.jarboot.core.cmd.express.Express;
import io.github.majianzheng.jarboot.core.cmd.express.ExpressException;
import io.github.majianzheng.jarboot.core.cmd.express.ExpressFactory;
import io.github.majianzheng.jarboot.core.cmd.model.ClassLoaderVO;
import io.github.majianzheng.jarboot.core.cmd.model.OgnlModel;
import io.github.majianzheng.jarboot.core.constant.CoreConstant;
import io.github.majianzheng.jarboot.core.utils.ClassLoaderUtils;
import io.github.majianzheng.jarboot.core.utils.ClassUtils;
import io.github.majianzheng.jarboot.core.utils.LogUtils;
import org.slf4j.Logger;

import java.lang.instrument.Instrumentation;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author majianzheng
 *
 */
@Name("ognl")
@Summary("Execute ognl expression.")
@Description(CoreConstant.EXAMPLE
                + "  ognl '@java.lang.System@out.println(\"hello\")' \n"
                + "  ognl -x 2 '@Singleton@getInstance()' \n"
                + "  ognl '@Demo@staticFiled' \n"
                + "  ognl '#value1=@System@getProperty(\"java.home\"), #value2=@System@getProperty(\"java.runtime.name\"), {#value1, #value2}'\n"
                + "  ognl -c 5d113a51 '@io.github.majianzheng.jarboot.core.GlobalOptions@isDump' \n"
                + CoreConstant.WIKI + CoreConstant.WIKI_HOME + "ognl\n"
                + "  https://commons.apache.org/proper/commons-ognl/language-guide.html")
public class OgnlCommand extends AbstractCommand {
    private static final Logger logger = LogUtils.getLogger();

    private String express;
    private String hashCode;
    private String classLoaderClass;
    private int expand = 1;

    @Argument(argName = "express", index = 0, required = true)
    @Description("The ognl expression.")
    public void setExpress(String express) {
        this.express = express;
    }

    @Option(shortName = "c", longName = "classLoader")
    @Description("The hash code of the special class's classLoader, default classLoader is SystemClassLoader.")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (1 by default).")
    public void setExpand(Integer expand) {
        this.expand = expand;
    }

    @Override
    public void run() {
        Instrumentation inst = EnvironmentContext.getInstrumentation();
        ClassLoader classLoader = null;
        if (hashCode != null) {
            classLoader = ClassLoaderUtils.getClassLoader(inst, hashCode);
            if (classLoader == null) {
                session.end(false, "Can not find classloader with hashCode: " + hashCode + ".");
                return;
            }
        } else if (classLoaderClass != null) {
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
            if (matchedClassLoaders.size() == 1) {
                classLoader = matchedClassLoaders.get(0);
            } else if (matchedClassLoaders.size() > 1) {
                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                OgnlModel ognlModel = new OgnlModel()
                        .setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                session.appendResult(ognlModel);
                session.end(false, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                return;
            } else {
                session.end(false, "Can not find classloader by class name: " + classLoaderClass + ".");
                return;
            }
        } else {
            classLoader = ClassLoader.getSystemClassLoader();
        }

        Express unpooledExpress = ExpressFactory.unpooledExpress(classLoader);
        try {
            Object value = unpooledExpress.get(express);
            OgnlModel ognlModel = new OgnlModel()
                    .setValue(value)
                    .setExpand(expand);
            session.appendResult(ognlModel);
            session.end();
        } catch (ExpressException e) {
            logger.warn("ognl: failed execute express: " + express, e);
            session.end(false, "Failed to execute ognl, exception message: " + e.getMessage()
                    + ", please check logs for more details. ");
        }
    }
}
