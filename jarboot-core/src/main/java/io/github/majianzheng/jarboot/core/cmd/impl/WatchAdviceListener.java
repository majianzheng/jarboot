package io.github.majianzheng.jarboot.core.cmd.impl;

import io.github.majianzheng.jarboot.core.advisor.AccessPoint;
import io.github.majianzheng.jarboot.core.advisor.Advice;
import io.github.majianzheng.jarboot.core.advisor.AdviceListenerAdapter;
import io.github.majianzheng.jarboot.core.advisor.JarbootMethod;
import io.github.majianzheng.jarboot.core.session.AbstractCommandSession;
import io.github.majianzheng.jarboot.core.utils.LogUtils;
import io.github.majianzheng.jarboot.core.utils.ThreadLocalWatch;
import io.github.majianzheng.jarboot.core.cmd.model.WatchModel;
import org.slf4j.Logger;
import java.util.Date;

/**
 * @author majianzheng
 *
 */
@SuppressWarnings("squid:S1181")
class WatchAdviceListener extends AdviceListenerAdapter {
    private static final Logger logger = LogUtils.getLogger();
    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();
    private WatchCommand command;
    private AbstractCommandSession process;

    public WatchAdviceListener(WatchCommand command, AbstractCommandSession process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    private boolean isFinish() {
        return command.isFinish() || !command.isBefore() && !command.isException() && !command.isSuccess();
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, JarbootMethod method, Object target, Object[] args)
            throws Throwable {
        // 开始计算本次方法调用耗时
        threadLocalWatch.start();
        if (command.isBefore()) {
            watching(Advice.newForBefore(loader, clazz, method, target, args));
        }
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, JarbootMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        Advice advice = Advice.newForAfterRetuning(loader, clazz, method, target, args, returnObject);
        if (command.isSuccess()) {
            watching(advice);
        }

        finishing(advice);
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, JarbootMethod method, Object target, Object[] args,
                              Throwable throwable) {
        Advice advice = Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable);
        if (command.isException()) {
            watching(advice);
        }

        finishing(advice);
    }

    private void finishing(Advice advice) {
        if (isFinish()) {
            watching(advice);
        }
    }


    private void watching(Advice advice) {
        try {
            // 本次调用的耗时
            double cost = threadLocalWatch.costInMillis();
            boolean conditionResult = isConditionMet(command.getConditionExpress(), advice, cost);
            if (this.isVerbose()) {
                process.console("Condition express: " + command.getConditionExpress() + " , result: " + conditionResult + "\n");
            }
            if (conditionResult) {
                Object value = getExpressionResult(command.getExpress(), advice, cost);

                WatchModel model = new WatchModel();
                model.setTs(new Date());
                model.setCost(cost);
                model.setValue(value);
                model.setExpand(command.getExpand());
                model.setSizeLimit(command.getSizeLimit());
                model.setClassName(advice.getClazz().getName());
                model.setMethodName(advice.getMethod().getName());
                if (advice.isBefore()) {
                    model.setAccessPoint(AccessPoint.ACCESS_BEFORE.getKey());
                } else if (advice.isAfterReturning()) {
                    model.setAccessPoint(AccessPoint.ACCESS_AFTER_RETUNING.getKey());
                } else if (advice.isAfterThrowing()) {
                    model.setAccessPoint(AccessPoint.ACCESS_AFTER_THROWING.getKey());
                }

                process.appendResult(model);
                process.times().incrementAndGet();
                if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
                    abortProcess(process, command.getNumberOfLimit());
                }
            }
        } catch (Throwable e) {
            logger.warn("watch failed.", e);
            process.end(false, "watch failed, condition is: " +
                    command.getConditionExpress() + ", express is: " + command.getExpress() + ", " +
                    e.getMessage() + ", visit log file for more details.");
        }
    }
}
