package io.github.majianzheng.jarboot.core.cmd.impl;

import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.api.cmd.annotation.*;
import io.github.majianzheng.jarboot.core.basic.EnvironmentContext;
import io.github.majianzheng.jarboot.core.cmd.AbstractCommand;
import io.github.majianzheng.jarboot.core.cmd.model.ClassLoaderVO;
import io.github.majianzheng.jarboot.core.cmd.model.MethodVO;
import io.github.majianzheng.jarboot.core.cmd.model.RowAffectModel;
import io.github.majianzheng.jarboot.core.cmd.model.SearchMethodModel;
import io.github.majianzheng.jarboot.core.constant.CoreConstant;
import io.github.majianzheng.jarboot.core.utils.ClassLoaderUtils;
import io.github.majianzheng.jarboot.core.utils.ClassUtils;
import io.github.majianzheng.jarboot.core.utils.LogUtils;
import io.github.majianzheng.jarboot.core.utils.SearchUtils;
import io.github.majianzheng.jarboot.core.utils.affect.RowAffect;
import io.github.majianzheng.jarboot.core.utils.matcher.Matcher;
import io.github.majianzheng.jarboot.core.utils.matcher.RegexMatcher;
import io.github.majianzheng.jarboot.core.utils.matcher.WildcardMatcher;
import org.slf4j.Logger;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 展示方法信息
 *
 * @author majianzheng
 */
@Name("sm")
@Summary("Search the method of classes loaded by JVM")
@Description(CoreConstant.EXAMPLE +
        "  sm java.lang.String\n" +
        "  sm -d org.apache.commons.lang.StringUtils\n" +
        "  sm -d org/apache/commons/lang/StringUtils\n" +
        "  sm *StringUtils *\n" +
        "  sm -Ed org\\\\.apache\\\\.commons\\\\.lang\\.StringUtils .*\n" +
        CoreConstant.WIKI + CoreConstant.WIKI_HOME + "sm")
@SuppressWarnings({"squid:S3776", "unused"})
public class SearchMethodCommand extends AbstractCommand {
    private static final Logger logger = LogUtils.getLogger();

    private String classPattern;
    private String methodPattern;
    private String hashCode = null;
    private String classLoaderClass;
    private boolean isDetail = false;
    private boolean isRegEx = false;
    private int numberOfLimit = 100;

    @Argument(argName = "class-pattern", index = 0)
    @Description("Class name pattern, use either '.' or '/' as separator")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Argument(argName = "method-pattern", index = 1, required = false)
    @Description("Method name pattern")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    @Option(shortName = "d", longName = "details", flag = true)
    @Description("Display the details of method")
    public void setDetail(boolean detail) {
        isDetail = detail;
    }

    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special class's classLoader")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    @Option(shortName = "n", longName = "limits")
    @Description("Maximum number of matching classes (100 by default)")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    @Override
    public void run() {
        RowAffect affect = new RowAffect();

        Instrumentation inst = EnvironmentContext.getInstrumentation();
        Matcher<String> methodNameMatcher = methodNameMatcher();
        
        if (hashCode == null && classLoaderClass != null) {
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
            if (matchedClassLoaders.size() == 1) {
                hashCode = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
            } else if (matchedClassLoaders.size() > 1) {
                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                SearchMethodModel searchmethodModel = new SearchMethodModel()
                        .setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                session.appendResult(searchmethodModel);
                session.end(false, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                return;
            } else {
                session.end(false, "Can not find classloader by class name: " + classLoaderClass + ".");
                return;
            }
        }

        Set<Class<?>> matchedClasses = SearchUtils.searchClass(inst, classPattern, isRegEx, hashCode);

        if (numberOfLimit > 0 && matchedClasses.size() > numberOfLimit) {
            session.end(false, "The number of matching classes is greater than : " + numberOfLimit+". \n" +
                    "Please specify a more accurate 'class-patten' or use the parameter '-n' to change the maximum number of matching classes.");
            return;
        }
        for (Class<?> clazz : matchedClasses) {
            try {
                for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                    if (!methodNameMatcher.matching("<init>")) {
                        continue;
                    }

                    MethodVO methodInfo = ClassUtils.createMethodInfo(constructor, clazz, isDetail);
                    session.appendResult(new SearchMethodModel(methodInfo, isDetail));
                    affect.rCnt(1);
                }

                for (Method method : clazz.getDeclaredMethods()) {
                    if (!methodNameMatcher.matching(method.getName())) {
                        continue;
                    }
                    MethodVO methodInfo = ClassUtils.createMethodInfo(method, clazz, isDetail);
                    session.appendResult(new SearchMethodModel(methodInfo, isDetail));
                    affect.rCnt(1);
                }
            } catch (Exception e) {
                //print failed className
                String msg = String.format("process class failed: %s, error: %s", clazz.getName(), e.getMessage());
                logger.error(msg, e);
                session.end(false, msg);
                return;
            }
        }

        session.appendResult(new RowAffectModel(affect));
        session.end();
    }

    private Matcher<String> methodNameMatcher() {
        // auto fix default methodPattern
        if (StringUtils.isBlank(methodPattern)) {
            methodPattern = isRegEx ? ".*" : "*";
        }
        return isRegEx ? new RegexMatcher(methodPattern) : new WildcardMatcher(methodPattern);
    }
}
