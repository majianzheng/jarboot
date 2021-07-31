package com.mz.jarboot.core.session;

import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.core.advisor.AdviceListener;
import com.mz.jarboot.core.cmd.model.ResultModel;

import java.lang.instrument.ClassFileTransformer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author majianzheng
 */
public interface CommandCoreSession extends CommandSession {

    /**
     * 获取会话id
     * @return 会话id
     */
    @Override
    String getSessionId();

    /**
     * 每执行一次命令生成一个唯一id
     * @return job id
     */
    @Override
    String getJobId();

    /**
     * 是否运行中
     * @return 是否在允许
     */
    boolean isRunning();

    /**
     * 开始执行
     */
    void setRunning();

    /**
     * 控制台消息打印
     * @param text 消息
     */
    @Override
    void console(String text);

    /**
     * 返回执行结果
     * @param resultModel 执行结果
     */
    void appendResult(ResultModel resultModel);

    /**
     * 注册监视器
     * @param adviceListener 监视器
     * @param transformer transformer
     */
    void register(AdviceListener adviceListener, ClassFileTransformer transformer);

    /**
     * 次数
     * @return 次数
     */
    AtomicInteger times();

    /**
     * 取消
     */
    @Override
    void cancel();

    /**
     * 结束
     */
    @Override
    void end();

    /**
     * 结束
     * @param success 是否成功
     */
    @Override
    void end(boolean success);

    /**
     * 结束
     * @param success 是否成功
     * @param message 消息
     */
    @Override
    void end(boolean success, String message);
}
