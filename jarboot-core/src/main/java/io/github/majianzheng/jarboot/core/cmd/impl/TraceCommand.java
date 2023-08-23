package io.github.majianzheng.jarboot.core.cmd.impl;

import io.github.majianzheng.jarboot.api.cmd.annotation.*;
import io.github.majianzheng.jarboot.core.GlobalOptions;
import io.github.majianzheng.jarboot.core.advisor.AdviceListener;
import io.github.majianzheng.jarboot.core.constant.CoreConstant;
import io.github.majianzheng.jarboot.core.session.AbstractCommandSession;
import io.github.majianzheng.jarboot.core.utils.SearchUtils;
import io.github.majianzheng.jarboot.core.utils.matcher.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
@Name("trace")
@Summary("Trace the execution time of specified method invocation.")
@Description(value = CoreConstant.EXPRESS_DESCRIPTION + CoreConstant.EXAMPLE +
        "  trace org.apache.commons.lang.StringUtils isBlank\n" +
        "  trace *StringUtils isBlank\n" +
        "  trace *StringUtils isBlank params[0].length==1\n" +
        "  trace *StringUtils isBlank '#cost>100'\n" +
        "  trace -E org\\\\.apache\\\\.commons\\\\.lang\\\\.StringUtils isBlank\n" +
        "  trace -E com.test.ClassA|org.test.ClassB method1|method2|method3\n" +
        "  trace demo.MathGame run -n 5\n" +
        "  trace demo.MathGame run --skipJDKMethod false\n" +
        "  trace javax.servlet.Filter * --exclude-class-pattern com.demo.TestFilter\n" +
        CoreConstant.WIKI + CoreConstant.WIKI_HOME + "trace")
public class TraceCommand extends EnhancerCommand {
    private String classPattern;
    private String methodPattern;
    private String conditionExpress;
    private boolean isRegEx = false;
    private int numberOfLimit = 100;
    private List<String> pathPatterns;
    private boolean skipJDKTrace;

    @Argument(argName = "class-pattern", index = 0)
    @Description("Class name pattern, use either '.' or '/' as separator")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Argument(argName = "method-pattern", index = 1)
    @Description("Method name pattern")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    @Argument(argName = "condition-express", index = 2, required = false)
    @Description(CoreConstant.CONDITION_EXPRESS)
    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
    }

    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    @Option(shortName = "n", longName = "limits")
    @Description("Threshold of execution times")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    @Option(shortName = "p", longName = "path", acceptMultipleValues = true)
    @Description("path tracing pattern")
    public void setPathPatterns(List<String> pathPatterns) {
        this.pathPatterns = pathPatterns;
    }

    @Option(longName = "skipJDKMethod")
    @DefaultValue("true")
    @Description("skip jdk method trace, default value true.")
    public void setSkipJDKTrace(boolean skipJDKTrace) {
        this.skipJDKTrace = skipJDKTrace;
    }

    public String getClassPattern() {
        return classPattern;
    }

    public String getMethodPattern() {
        return methodPattern;
    }

    public String getConditionExpress() {
        return conditionExpress;
    }

    public boolean isSkipJDKTrace() {
        return skipJDKTrace;
    }

    public boolean isRegEx() {
        return isRegEx;
    }

    public int getNumberOfLimit() {
        return numberOfLimit;
    }

    public List<String> getPathPatterns() {
        return pathPatterns;
    }

    @Override
    protected Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            if (pathPatterns == null || pathPatterns.isEmpty()) {
                classNameMatcher = SearchUtils.classNameMatcher(getClassPattern(), isRegEx());
            } else {
                classNameMatcher = getPathTracingClassMatcher();
            }
        }
        return classNameMatcher;
    }

    @Override
    protected Matcher getClassNameExcludeMatcher() {
        if (classNameExcludeMatcher == null && getExcludeClassPattern() != null) {
            classNameExcludeMatcher = SearchUtils.classNameMatcher(getExcludeClassPattern(), isRegEx());
        }
        return classNameExcludeMatcher;
    }

    @Override
    protected Matcher getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            if (pathPatterns == null || pathPatterns.isEmpty()) {
                methodNameMatcher = SearchUtils.classNameMatcher(getMethodPattern(), isRegEx());
            } else {
                methodNameMatcher = getPathTracingMethodMatcher();
            }
        }
        return methodNameMatcher;
    }


    @Override
    protected AdviceListener getAdviceListener(AbstractCommandSession process) {
        if (pathPatterns == null || pathPatterns.isEmpty()) {
            return new TraceAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
        } else {
            return new PathTraceAdviceListener(this, process);
        }
    }

    /**
     * 构造追踪路径匹配
     */
    private Matcher<String> getPathTracingClassMatcher() {

        List<Matcher<String>> matcherList = new ArrayList<>();
        matcherList.add(SearchUtils.classNameMatcher(getClassPattern(), isRegEx()));

        if (null != getPathPatterns()) {
            for (String pathPattern : getPathPatterns()) {
                if (isRegEx()) {
                    matcherList.add(new RegexMatcher(pathPattern));
                } else {
                    matcherList.add(new WildcardMatcher(pathPattern));
                }
            }
        }

        return new GroupMatcher.Or<>(matcherList);
    }

    private Matcher<String> getPathTracingMethodMatcher() {
        return new TrueMatcher<>();
    }
}
