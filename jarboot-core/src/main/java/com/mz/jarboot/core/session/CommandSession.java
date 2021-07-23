package com.mz.jarboot.core.session;

import com.mz.jarboot.core.advisor.AdviceListener;
import com.mz.jarboot.core.cmd.model.ResultModel;

import java.lang.instrument.ClassFileTransformer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author majianzheng
 */
public interface CommandSession {

    /**
     * 获取会话id
     * @return 会话id
     */
    String getSessionId();

    /**
     * 每执行一次命令生成一个唯一id
     * @return job id
     */
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
     * 应答
     * @param message 消息
     */
    void ack(String message);

    /**
     * 控制台消息打印
     * @param text 消息
     */
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
    void cancel();

    /**
     * 结束
     */
    void end();

    /**
     * 结束
     * @param success 是否成功
     */
    void end(boolean success);

    /**
     * 结束
     * @param success 是否成功
     * @param message 消息
     */
    void end(boolean success, String message);
}
