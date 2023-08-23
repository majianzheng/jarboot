package io.github.majianzheng.jarboot.core.cmd.impl;

import io.github.majianzheng.jarboot.common.Pair;
import io.github.majianzheng.jarboot.api.cmd.annotation.*;
import io.github.majianzheng.jarboot.core.basic.EnvironmentContext;
import io.github.majianzheng.jarboot.core.cmd.AbstractCommand;
import io.github.majianzheng.jarboot.core.constant.CoreConstant;
import io.github.majianzheng.jarboot.core.utils.*;
import io.github.majianzheng.jarboot.core.utils.affect.RowAffect;
import io.github.majianzheng.jarboot.core.cmd.model.ClassLoaderVO;
import io.github.majianzheng.jarboot.core.cmd.model.ClassVO;
import io.github.majianzheng.jarboot.core.cmd.model.JadModel;
import io.github.majianzheng.jarboot.core.cmd.model.RowAffectModel;
import org.slf4j.Logger;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author majianzheng
 */
@SuppressWarnings("squid:S1181")
@Name("jad")
@Summary("Decompile class")
@Description(CoreConstant.EXAMPLE +
        "  jad java.lang.String\n" +
        "  jad java.lang.String toString\n" +
        "  jad --source-only java.lang.String\n" +
        "  jad -c 39eb305e org/apache/log4j/Logger\n" +
        "  jad -c 39eb305e -E org\\\\.apache\\\\.*\\\\.StringUtils\n" +
        CoreConstant.WIKI + CoreConstant.WIKI_HOME + "jad")
public class JadCommand extends AbstractCommand {
    private static final Logger logger = LogUtils.getLogger();
    private static Pattern pattern = Pattern.compile("(?m)^/\\*\\s*\\*/\\s*$" + System.getProperty("line.separator"));

    private String classPattern;
    private String methodName;
    private String code = null;
    private String classLoaderClass;
    private boolean isRegEx = false;
    private boolean hideUnicode = false;
    private boolean lineNumber;

    /**
     * jad output source code only
     */
    private boolean sourceOnly = false;

    @Argument(argName = "class-pattern", index = 0)
    @Description("Class name pattern, use either '.' or '/' as separator")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Argument(argName = "method-name", index = 1, required = false)
    @Description("method name pattern, decompile a specific method instead of the whole class")
    public void setMethodName(String methodName) {
        this.methodName = methodName;
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

    @Option(longName = "hideUnicode", flag = true)
    @Description("hide unicode, default value false")
    public void setHideUnicode(boolean hideUnicode) {
        this.hideUnicode = hideUnicode;
    }

    @Option(longName = "source-only", flag = true)
    @Description("Output source code only")
    public void setSourceOnly(boolean sourceOnly) {
        this.sourceOnly = sourceOnly;
    }

    @Option(longName = "lineNumber")
    @DefaultValue("true")
    @Description("Output source code contins line number, default value true")
    public void setLineNumber(boolean lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public void run() {
        RowAffect affect = new RowAffect();
        Instrumentation inst = EnvironmentContext.getInstrumentation();

        if (code == null && classLoaderClass != null) {
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
            if (matchedClassLoaders.size() == 1) {
                code = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
            } else if (matchedClassLoaders.size() > 1) {
                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                JadModel jadModel = new JadModel()
                        .setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                session.appendResult(jadModel);
                session.end(false, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                return;
            } else {
                session.end(false, "Can not find classloader by class name: " + classLoaderClass + ".");
                return;
            }
        }
        
        Set<Class<?>> matchedClasses = SearchUtils.searchClassOnly(inst, classPattern, isRegEx, code);

        try {
            boolean success = true;
            StringBuilder sb = new StringBuilder();
            if (matchedClasses == null || matchedClasses.isEmpty()) {
                success = processNoMatch(sb);
            } else if (matchedClasses.size() > 1) {
                success = processMatches(sb, matchedClasses);
            } else { // matchedClasses size is 1
                // find inner classes.
                Set<Class<?>> withInnerClasses = SearchUtils.searchClassOnly(inst,  matchedClasses.iterator().next().getName() + "$*", false, code);
                if(withInnerClasses.isEmpty()) {
                    withInnerClasses = matchedClasses;
                }
                success = processExactMatch(sb, affect, inst, matchedClasses, withInnerClasses);
            }
            if (!this.sourceOnly) {
                session.appendResult(new RowAffectModel(affect));
            }
            session.end(success, sb.toString());
        } catch (Throwable e){
            logger.error("processing error", e);
            session.end(false, "processing error");
        }
    }

    private boolean processExactMatch(StringBuilder sb, RowAffect affect, Instrumentation inst, Set<Class<?>> matchedClasses, Set<Class<?>> withInnerClasses) {
        Class<?> c = matchedClasses.iterator().next();
        Set<Class<?>> allClasses = new HashSet<>(withInnerClasses);
        allClasses.add(c);

        try {
            ClassDumpTransformer transformer = new ClassDumpTransformer(allClasses);
            InstrumentationUtils.retransformClasses(inst, transformer, allClasses);

            Map<Class<?>, File> classFiles = transformer.getDumpResult();
            File classFile = classFiles.get(c);

            Pair<String,NavigableMap<Integer,Integer>> decompileResult = Decompiler.decompileWithMappings(classFile.getAbsolutePath(), methodName, hideUnicode, lineNumber);
            String source = decompileResult.getFirst();
            if (source != null) {
                source = pattern.matcher(source).replaceAll("");
            } else {
                source = "unknown";
            }

            JadModel jadModel = new JadModel();
            jadModel.setSource(source);
            jadModel.setMappings(decompileResult.getSecond());
            if (!this.sourceOnly) {
                jadModel.setClassInfo(ClassUtils.createSimpleClassInfo(c));
                jadModel.setLocation(ClassUtils.getCodeSource(c.getProtectionDomain().getCodeSource()));
            }
            session.appendResult(jadModel);

            affect.rCnt(classFiles.keySet().size());
            return true;
        } catch (Throwable t) {
            logger.error("jad: fail to decompile class: " + c.getName(), t);
            sb.append("jad: fail to decompile class: ").append(c.getName());
            return false;
        }
    }

    private boolean processMatches(StringBuilder sb, Set<Class<?>> matchedClasses) {

        String usage = "jad -c <hashcode> " + classPattern;
        String msg = " Found more than one class for: " + classPattern + ", Please use " + usage;
        session.console(msg);

        List<ClassVO> classVOs = ClassUtils.createClassVOList(matchedClasses);
        JadModel jadModel = new JadModel();
        jadModel.setMatchedClasses(classVOs);
        session.appendResult(jadModel);

        sb.append(msg);
        return false;
    }

    private boolean processNoMatch(StringBuilder sb) {
        sb.append("No class found for: ").append(classLoaderClass);
        return false;
    }
}
