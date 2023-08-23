package io.github.majianzheng.jarboot.core.advisor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知编织者<br/>
 * <p/>
 * <h2>线程帧栈与执行帧栈</h2>
 * 编织者在执行通知的时候有两个重要的栈:线程帧栈(threadFrameStack),执行帧栈(frameStack)
 * <p/>
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class AdviceWeaver {
    /**
     * 通知监听器集合
     */
    private static final Map<Long, AdviceListener> ADVICES = new ConcurrentHashMap<>();

    /**
     * 注册监听器
     *
     * @param listener 通知监听器
     */
    public static void reg(AdviceListener listener) {

        // 触发监听器创建
        listener.create();

        // 注册监听器
        ADVICES.put(listener.id(), listener);
    }

    /**
     * 注销监听器
     *
     * @param listener 监听器
     */
    public static void unReg(AdviceListener listener) {
        if (null != listener) {
            // 注销监听器
            ADVICES.remove(listener.id());

            // 触发监听器销毁
            listener.destroy();
        }
    }

    public static AdviceListener listener(long id) {
        return ADVICES.get(id);
    }

    /**
     * 恢复监听
     *
     * @param listener 通知监听器
     */
    public static void resume(AdviceListener listener) {
        // 注册监听器
        ADVICES.put(listener.id(), listener);
    }

    /**
     * 暂停监听
     *
     * @param adviceId 通知ID
     */
    public static AdviceListener suspend(long adviceId) {
        // 注销监听器
        return ADVICES.remove(adviceId);
    }

    private AdviceWeaver() {}
}
