package io.github.majianzheng.jarboot.core.cmd.impl;

import io.github.majianzheng.jarboot.core.advisor.Advice;
import io.github.majianzheng.jarboot.core.advisor.AdviceListenerAdapter;
import io.github.majianzheng.jarboot.core.advisor.JarbootMethod;
import io.github.majianzheng.jarboot.core.session.AbstractCommandSession;
import io.github.majianzheng.jarboot.core.utils.LogUtils;
import io.github.majianzheng.jarboot.core.utils.ThreadLocalWatch;
import org.slf4j.Logger;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
@SuppressWarnings("squid:S1181")
public class AbstractTraceAdviceListener extends AdviceListenerAdapter {
    private static final Logger logger = LogUtils.getLogger();
    protected final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();
    protected TraceCommand command;
    protected AbstractCommandSession process;

    protected final ThreadLocal<TraceEntity> threadBoundEntity = new ThreadLocal<>();

    /**
     * Constructor
     */
    public AbstractTraceAdviceListener(TraceCommand command, AbstractCommandSession process) {
        this.command = command;
        this.process = process;
    }

    protected TraceEntity threadLocalTraceEntity(ClassLoader loader) {
        TraceEntity traceEntity = threadBoundEntity.get();
        if (traceEntity == null) {
            traceEntity = new TraceEntity(loader);
            threadBoundEntity.set(traceEntity);
        }
        return traceEntity;
    }

    @Override
    public void destroy() {
        threadBoundEntity.remove();
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, JarbootMethod method, Object target, Object[] args) {
        TraceEntity traceEntity = threadLocalTraceEntity(loader);
        traceEntity.tree.begin(clazz.getName(), method.getName(), -1, false);
        traceEntity.deep++;
        // 开始计算本次方法调用耗时
        threadLocalWatch.start();
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, JarbootMethod method, Object target, Object[] args,
                               Object returnObject) {
        threadLocalTraceEntity(loader).tree.end();
        final Advice advice = Advice.newForAfterRetuning(loader, clazz, method, target, args, returnObject);
        finishing(loader, advice);
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, JarbootMethod method, Object target, Object[] args,
                              Throwable throwable) throws Throwable {
        int lineNumber = -1;
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace.length != 0) {
            lineNumber = stackTrace[0].getLineNumber();
        }

        threadLocalTraceEntity(loader).tree.end(throwable, lineNumber);
        final Advice advice = Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable);
        finishing(loader, advice);
    }

    public TraceCommand getCommand() {
        return command;
    }

    private void finishing(ClassLoader loader, Advice advice) {
        // 本次调用的耗时
        TraceEntity traceEntity = threadLocalTraceEntity(loader);
        double cost = threadLocalWatch.costInMillis();
        if (--traceEntity.deep == 0) {
            try {
                boolean conditionResult = isConditionMet(command.getConditionExpress(), advice, cost);
                if (this.isVerbose()) {
                    process.console("Condition express: " + command.getConditionExpress() + " , result: " + conditionResult + "\n");
                }
                if (conditionResult) {
                    // 满足输出条件
                    process.times().incrementAndGet();
                    process.appendResult(traceEntity.getModel());

                    // 是否到达数量限制
                    if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
                        abortProcess(process, command.getNumberOfLimit());
                    }
                }
            } catch (Throwable e) {
                logger.warn("trace failed.", e);
                process.end(false, "trace failed, " + e.getMessage()
                              + ", visit log file for more details.");
            } finally {
                threadBoundEntity.remove();
            }
        }
    }
}
