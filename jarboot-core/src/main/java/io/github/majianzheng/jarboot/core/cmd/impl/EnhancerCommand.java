package io.github.majianzheng.jarboot.core.cmd.impl;

import io.github.majianzheng.jarboot.core.advisor.AdviceListener;
import io.github.majianzheng.jarboot.core.advisor.AdviceWeaver;
import io.github.majianzheng.jarboot.core.advisor.ClassEnhancer;
import io.github.majianzheng.jarboot.core.advisor.InvokeTraceable;
import io.github.majianzheng.jarboot.core.basic.EnvironmentContext;
import io.github.majianzheng.jarboot.core.cmd.AbstractCommand;
import io.github.majianzheng.jarboot.api.cmd.annotation.Description;
import io.github.majianzheng.jarboot.api.cmd.annotation.Option;
import io.github.majianzheng.jarboot.core.cmd.model.EnhancerModel;
import io.github.majianzheng.jarboot.core.session.AbstractCommandSession;
import io.github.majianzheng.jarboot.core.utils.LogUtils;
import io.github.majianzheng.jarboot.core.utils.affect.EnhancerAffect;
import io.github.majianzheng.jarboot.core.utils.matcher.Matcher;
import org.slf4j.Logger;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.List;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
@SuppressWarnings("java:S3740")
public abstract class EnhancerCommand extends AbstractCommand {
    private static final Logger logger = LogUtils.getLogger();
    protected static final List<String> EMPTY = Collections.emptyList();

    private String excludeClassPattern;

    protected Matcher classNameMatcher;
    protected Matcher classNameExcludeMatcher;
    protected Matcher methodNameMatcher;

    protected long listenerId;

    protected boolean verbose;

    @Option(longName = "exclude-class-pattern")
    @Description("exclude class name pattern, use either '.' or '/' as separator")
    public void setExcludeClassPattern(String excludeClassPattern) {
        this.excludeClassPattern = excludeClassPattern;
    }

    @Option(longName = "listenerId")
    @Description("The special listenerId")
    public void setListenerId(long listenerId) {
        this.listenerId = listenerId;
    }

    @Option(shortName = "v", longName = "verbose", flag = true)
    @Description("Enables print verbose information, default value false.")
    public void setVerbosee(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * 类名匹配
     *
     * @return 获取类名匹配
     */
    protected abstract Matcher getClassNameMatcher();

    /**
     * 排除类名匹配
     */
    protected abstract Matcher getClassNameExcludeMatcher();

    /**
     * 方法名匹配
     *
     * @return 获取方法名匹配
     */
    protected abstract Matcher getMethodNameMatcher();

    /**
     * 获取监听器
     *
     * @return 返回监听器
     */
    protected abstract AdviceListener getAdviceListener(AbstractCommandSession process);

    AdviceListener getAdviceListenerWithId(AbstractCommandSession process) {
        if (listenerId != 0) {
            AdviceListener listener = AdviceWeaver.listener(listenerId);
            if (listener != null) {
                return listener;
            }
        }
        return getAdviceListener(process);
    }
    @Override
    public void run() {
        // start to enhance
        enhance(session);
    }

    @SuppressWarnings("squid:S1181")
    protected void enhance(AbstractCommandSession process) {
        EnhancerAffect effect = null;
        try {
            Instrumentation inst = EnvironmentContext.getInstrumentation();
            AdviceListener listener = getAdviceListenerWithId(process);
            if (listener == null) {
                logger.error("advice listener is null");
                String msg = "advice listener is null, check jarboot log";
                session.appendResult(new EnhancerModel(effect, false, msg));
                session.end(false, msg);
                return;
            }
            boolean skipJDKTrace = false;
            if(listener instanceof AbstractTraceAdviceListener) {
                skipJDKTrace = ((AbstractTraceAdviceListener) listener).getCommand().isSkipJDKTrace();
            }

            ClassEnhancer enhancer = new ClassEnhancer(listener, listener instanceof InvokeTraceable,
                    skipJDKTrace, getClassNameMatcher(), getClassNameExcludeMatcher(), getMethodNameMatcher());
            // 注册通知监听器
            process.register(listener, enhancer);

            effect = enhancer.enhance(inst);

            if (effect.getThrowable() != null) {
                String msg = "error happens when enhancing class: "+effect.getThrowable().getMessage();
                process.appendResult(new EnhancerModel(effect, false, msg));
                process.end(true, msg + ", check jarboot log file.");
                return;
            }

            if (effect.cCnt() == 0 || effect.mCnt() == 0) {
                // no class effected
                // might be method code too large
                process.appendResult(new EnhancerModel(effect, false, "No class or method is affected"));

                String smCommand = "sm CLASS_NAME METHOD_NAME";
                String optionsCommand = "options unsafe true";
                String javaPackage = "java.*";
                String resetCommand = "reset CLASS_NAME";
                String logStr = "core.log";
                String msg = "No class or method is affected, try:\n"
                        + "1. Execute `" + smCommand + "` to make sure the method you are tracing actually exists (it might be in your parent class).\n"
                        + "2. Execute `" + optionsCommand + "`, if you want to enhance the classes under the `" + javaPackage + "` package.\n"
                        + "3. Execute `" + resetCommand + "` and try again, your method body might be too large.\n"
                        + "4. Check jarboot log: " + logStr + "\n";
                process.end(false, msg);
                return;
            }

            process.appendResult(new EnhancerModel(effect, true));

            //异步执行，在AdviceListener中结束
        } catch (Throwable e) {
            String msg = "error happens when enhancing class: "+e.getMessage();
            logger.error(msg, e);
            process.appendResult(new EnhancerModel(effect, false, msg));
            process.end(false, msg);
        }
    }

    public String getExcludeClassPattern() {
        return excludeClassPattern;
    }
}
