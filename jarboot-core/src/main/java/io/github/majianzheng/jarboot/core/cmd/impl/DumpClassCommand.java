package io.github.majianzheng.jarboot.core.cmd.impl;

import io.github.majianzheng.jarboot.api.cmd.annotation.*;
import io.github.majianzheng.jarboot.core.basic.EnvironmentContext;
import io.github.majianzheng.jarboot.core.cmd.AbstractCommand;
import io.github.majianzheng.jarboot.core.constant.CoreConstant;
import io.github.majianzheng.jarboot.core.utils.*;
import io.github.majianzheng.jarboot.core.utils.affect.RowAffect;
import io.github.majianzheng.jarboot.core.cmd.model.*;
import org.slf4j.Logger;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.*;


/**
 * Dump class byte array
 * @author majianzheng
 */
@Name("dump")
@Summary("Dump class byte array from JVM")
@Description(CoreConstant.EXAMPLE +
        "  dump java.lang.String\n" +
        "  dump -d /tmp/output java.lang.String\n" +
        "  dump org/apache/commons/lang/StringUtils\n" +
        "  dump *StringUtils\n" +
        "  dump -E org\\\\.apache\\\\.commons\\\\.lang\\\\.StringUtils\n" +
        CoreConstant.WIKI + CoreConstant.WIKI_HOME + "dump")
public class DumpClassCommand extends AbstractCommand {
    private static final Logger logger = LogUtils.getLogger();

    private String classPattern;
    private String code = null;
    private String classLoaderClass;
    private boolean isRegEx = false;

    private int limit;

    @Argument(index = 0, argName = "class-pattern")
    @Description("Class name pattern, use either '.' or '/' as separator")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Option(shortName = "c", longName = "code")
    @Description("The hash code of the special class's classLoader")
    public void setCode(String code) {
        this.code = code;
    }

    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }
    
    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    @Option(shortName = "l", longName = "limit")
    @Description("The limit of dump classes size, default value is 5")
    @DefaultValue("50")
    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public void run() {
        RowAffect effect = new RowAffect();
        try {
            Instrumentation inst = EnvironmentContext.getInstrumentation();
            if (code == null && classLoaderClass != null) {
                List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
                if (matchedClassLoaders.size() == 1) {
                    code = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
                } else if (matchedClassLoaders.size() > 1) {
                    Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                    DumpClassModel dumpClassModel = new DumpClassModel()
                            .setClassLoaderClass(classLoaderClass)
                            .setMatchedClassLoaders(classLoaderVOList);
                    session.appendResult(dumpClassModel);
                    session.end(false, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                    return;
                } else {
                    session.end(false, "Can not find classloader by class name: " + classLoaderClass + ".");
                    return;
                }
            }

            Set<Class<?>> matchedClasses = SearchUtils.searchClass(inst, classPattern, isRegEx, code);
            if (matchedClasses == null || matchedClasses.isEmpty()) {
                session.end(false, "No class found for: " + classPattern);
            } else if (matchedClasses.size() > limit) {
                processMatches(matchedClasses);
            } else {
                processMatch(effect, inst, matchedClasses);
            }
            session.appendResult(new RowAffectModel(effect));
        } catch (Exception e){
            logger.error("processing error", e);
            session.end(false, "processing error");
        } finally {
            session.end();
        }
    }

    private void processMatch(RowAffect effect, Instrumentation inst, Set<Class<?>> matchedClasses) {
        try {
            Map<Class<?>, File> classFiles = dump(inst, matchedClasses);
            List<DumpClassVO> dumpedClasses = new ArrayList<>(classFiles.size());
            for (Map.Entry<Class<?>, File> entry : classFiles.entrySet()) {
                Class<?> clazz = entry.getKey();
                File file = entry.getValue();
                DumpClassVO dumpClassVO = new DumpClassVO();
                dumpClassVO.setLocation(file.getCanonicalPath());
                ClassUtils.fillSimpleClassVO(clazz, dumpClassVO);
                dumpedClasses.add(dumpClassVO);
            }
            session.appendResult(new DumpClassModel().setDumpedClasses(dumpedClasses));

            effect.rCnt(classFiles.keySet().size());
        } catch (Exception t) {
            String msg = "dump: fail to dump classes: " + matchedClasses;
            logger.error(msg, t);
            session.end(false, msg);
        }
    }

    private void processMatches(Set<Class<?>> matchedClasses) {
        String msg = String.format(
                "Found more than %d class for: %s, Please Try to specify the classloader with the -c option, or try to use --limit option.",
                limit, classPattern);
        session.console(msg);

        List<ClassVO> classVOs = ClassUtils.createClassVOList(matchedClasses);
        session.appendResult(new DumpClassModel().setMatchedClasses(classVOs));
        session.end(false, msg);
    }

    private Map<Class<?>, File> dump(Instrumentation inst, Set<Class<?>> classes) {
        ClassDumpTransformer transformer = new ClassDumpTransformer(classes);
        InstrumentationUtils.retransformClasses(inst, transformer, classes);
        return transformer.getDumpResult();
    }
}
