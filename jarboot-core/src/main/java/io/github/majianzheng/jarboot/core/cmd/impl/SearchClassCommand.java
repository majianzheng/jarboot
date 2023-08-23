package io.github.majianzheng.jarboot.core.cmd.impl;


import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.api.cmd.annotation.*;
import io.github.majianzheng.jarboot.core.basic.EnvironmentContext;
import io.github.majianzheng.jarboot.core.cmd.AbstractCommand;
import io.github.majianzheng.jarboot.core.cmd.model.ClassDetailVO;
import io.github.majianzheng.jarboot.core.cmd.model.ClassLoaderVO;
import io.github.majianzheng.jarboot.core.cmd.model.RowAffectModel;
import io.github.majianzheng.jarboot.core.cmd.model.SearchClassModel;
import io.github.majianzheng.jarboot.core.constant.CoreConstant;
import io.github.majianzheng.jarboot.core.session.Completion;
import io.github.majianzheng.jarboot.core.utils.ClassLoaderUtils;
import io.github.majianzheng.jarboot.core.utils.ClassUtils;
import io.github.majianzheng.jarboot.core.utils.ResultUtils;
import io.github.majianzheng.jarboot.core.utils.SearchUtils;
import io.github.majianzheng.jarboot.core.utils.affect.RowAffect;

import java.lang.instrument.Instrumentation;
import java.util.*;

/**
 * 展示类信息
 *
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
@Name("sc")
@Summary("Search all the classes loaded by JVM")
@Description(CoreConstant.EXAMPLE +
        "  sc -d org.apache.commons.lang.StringUtils\n" +
        "  sc -d org/apache/commons/lang/StringUtils\n" +
        "  sc -d *StringUtils\n" +
        "  sc -d -f org.apache.commons.lang.StringUtils\n" +
        "  sc -E org\\\\.apache\\\\.commons\\\\.lang\\\\.StringUtils\n" +
        CoreConstant.WIKI + CoreConstant.WIKI_HOME + "sc")
public class SearchClassCommand extends AbstractCommand {
    private String classPattern;
    private boolean isDetail = false;
    private boolean isField = false;
    private boolean isRegEx = false;
    private String hashCode = null;
    private String classLoaderClass;
    private Integer expand;
    private int numberOfLimit = 100;

    @Argument(argName = "class-pattern", index = 0)
    @Description("Class name pattern, use either '.' or '/' as separator")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Option(shortName = "d", longName = "details", flag = true)
    @Description("Display the details of class")
    public void setDetail(boolean detail) {
        isDetail = detail;
    }

    @Option(shortName = "f", longName = "field", flag = true)
    @Description("Display all the member variables")
    public void setField(boolean field) {
        isField = field;
    }

    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (0 by default)")
    public void setExpand(Integer expand) {
        this.expand = expand;
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
    @Description("Maximum number of matching classes with details (100 by default)")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    @Override
    public void run() {
        RowAffect affect = new RowAffect();
        Instrumentation inst = EnvironmentContext.getInstrumentation();

        if (hashCode == null && classLoaderClass != null) {
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
            if (matchedClassLoaders.size() == 1) {
                hashCode = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
            } else if (matchedClassLoaders.size() > 1) {
                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                SearchClassModel searchclassModel = new SearchClassModel()
                        .setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                session.appendResult(searchclassModel);
                session.end(false, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                return;
            } else {
                session.end(false, "Can not find classloader by class name: " + classLoaderClass + ".");
                return;
            }
        }

        List<Class<?>> matchedClasses = new ArrayList<>(SearchUtils.searchClass(inst, classPattern, isRegEx, hashCode));
        Collections.sort(matchedClasses, Comparator.comparing(StringUtils::classname));

        if (isDetail) {
            if (numberOfLimit > 0 && matchedClasses.size() > numberOfLimit) {
                session.end(false, "The number of matching classes is greater than : " + numberOfLimit+". \n" +
                        "Please specify a more accurate 'class-patten' or use the parameter '-n' to change the maximum number of matching classes.");
                return;
            }
            for (Class<?> clazz : matchedClasses) {
                ClassDetailVO classInfo = ClassUtils.createClassInfo(clazz, isField);
                session.appendResult(new SearchClassModel(classInfo, isDetail, isField, expand));
            }
        } else {
            int pageSize = 256;
            ResultUtils.processClassNames(matchedClasses, pageSize,
                (classNames, segment) -> {
                    session.appendResult(new SearchClassModel(classNames, segment));
                    return true;
                });
        }

        affect.rCnt(matchedClasses.size());
        session.appendResult(new RowAffectModel(affect));
        session.end();
    }

    @Override
    public void complete(Completion completion) {
        // do nothing 暂不实现
    }
}
