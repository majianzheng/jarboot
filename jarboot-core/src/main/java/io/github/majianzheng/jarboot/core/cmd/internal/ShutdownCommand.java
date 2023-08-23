package io.github.majianzheng.jarboot.core.cmd.internal;

import ch.qos.logback.classic.Logger;
import io.github.majianzheng.jarboot.api.cmd.annotation.Name;
import io.github.majianzheng.jarboot.api.cmd.annotation.Summary;
import io.github.majianzheng.jarboot.common.JarbootThreadFactory;
import io.github.majianzheng.jarboot.common.PidFileHelper;
import io.github.majianzheng.jarboot.core.advisor.ClassEnhancer;
import io.github.majianzheng.jarboot.core.basic.EnvironmentContext;
import io.github.majianzheng.jarboot.core.basic.WsClientFactory;
import io.github.majianzheng.jarboot.core.cmd.model.EnhancerModel;
import io.github.majianzheng.jarboot.core.stream.StdOutStreamReactor;
import io.github.majianzheng.jarboot.core.utils.LogUtils;
import io.github.majianzheng.jarboot.core.utils.affect.EnhancerAffect;
import io.github.majianzheng.jarboot.core.utils.matcher.WildcardMatcher;

/**
 * 关闭命令
 *
 * @author majianzheng
 */
@Name("shutdown")
@Summary("Shutdown all connection and exit the console")
public class ShutdownCommand extends AbstractInternalCommand {

    private static final Logger logger = LogUtils.getLogger();

    @Override
    public void run() {
        //尽可用于在线诊断进程
        if (!PidFileHelper.getServerPidString(EnvironmentContext.getAgentClient().getSid()).isEmpty()) {
            session.console("This command only use in `online diagnose` process.");
            session.console("命令仅可用于`在线诊断`的进程。");
            session.end();
            return;
        }
        try {
            // 退出之前需要重置所有的增强类
            session.console("Resetting all enhanced classes ...");
            EnhancerAffect enhancerAffect = ClassEnhancer
                    .reset(EnvironmentContext.getInstrumentation(), new WildcardMatcher("*"));
            session.appendResult(new EnhancerModel(enhancerAffect, true));
            session.console("Client is going to shutdown (not exit, just clean resource and disconnect)...");
        } catch (Exception e) {
            logger.error("An error occurred when stopping.", e);
            session.console("An error occurred when stopping.");
        } finally {
            session.end();
            JarbootThreadFactory
                    .createThreadFactory("jarboot.shutdown")
                    .newThread(this::destroyAll)
                    .start();
        }
    }
    
    private void destroyAll() {
        //关闭会话
        WsClientFactory.getInstance().closeSession();
        //恢复默认流
        StdOutStreamReactor.getInstance().enabled(false);
        //销毁线程池、会话等资源
        EnvironmentContext.destroy();
    }
}
