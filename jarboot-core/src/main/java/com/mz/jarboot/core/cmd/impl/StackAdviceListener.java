package com.mz.jarboot.core.cmd.impl;

import com.mz.jarboot.core.advisor.Advice;
import com.mz.jarboot.core.advisor.AdviceListenerAdapter;
import com.mz.jarboot.core.advisor.JarbootMethod;
import com.mz.jarboot.core.cmd.model.StackModel;
import com.mz.jarboot.core.session.CommandCoreSession;
import com.mz.jarboot.core.utils.LogUtils;
import com.mz.jarboot.core.utils.ThreadLocalWatch;
import com.mz.jarboot.core.utils.ThreadUtil;
import org.slf4j.Logger;
import java.util.Date;

/**
 * @author majianzheng
 */
@SuppressWarnings("all")
public class StackAdviceListener extends AdviceListenerAdapter {
    private static final Logger logger = LogUtils.getLogger();

    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();
    private StackCommand command;
    private CommandCoreSession process;

    public StackAdviceListener(StackCommand command, CommandCoreSession process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, JarbootMethod method, Object target, Object[] args)
            throws Throwable {
        // 开始计算本次方法调用耗时
        threadLocalWatch.start();
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, JarbootMethod method, Object target, Object[] args,
                              Throwable throwable) throws Throwable {
        Advice advice = Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable);
        finishing(advice);
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, JarbootMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        Advice advice = Advice.newForAfterRetuning(loader, clazz, method, target, args, returnObject);
        finishing(advice);
    }

    private void finishing(Advice advice) {
        // 本次调用的耗时
        try {
            double cost = threadLocalWatch.costInMillis();
            boolean conditionResult = isConditionMet(command.getConditionExpress(), advice, cost);
            if (this.isVerbose()) {
                process.console("Condition express: " + command.getConditionExpress() +
                        " , result: " + conditionResult + "\n");
            }
            if (conditionResult) {
                StackModel stackModel = ThreadUtil.getThreadStackModel(advice.getLoader(), Thread.currentThread());
                stackModel.setTs(new Date());
                process.appendResult(stackModel);
                process.times().incrementAndGet();
                if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
                    abortProcess(process, command.getNumberOfLimit());
                }
            }
        } catch (Throwable e) {
            logger.warn("stack failed.", e);
            process.end(false, "stack failed, condition is: " +
                    command.getConditionExpress() + ", " + e.getMessage() + ", visit logs for more details.");
        }
    }
}
