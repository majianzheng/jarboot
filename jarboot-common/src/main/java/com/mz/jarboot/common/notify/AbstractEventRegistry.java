package com.mz.jarboot.common.notify;

import com.mz.jarboot.api.constant.TaskLifecycle;
import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.common.utils.StringUtils;

/**
 * @author majianzheng
 */
public interface AbstractEventRegistry {
    byte[] SPLIT = new byte[]{'\r'};
    /**
     * 注册订阅者
     * @param topic 主题
     * @param subscriber 订阅者
     */
    void registerSubscriber(String topic, Subscriber<? extends JarbootEvent> subscriber);

    /**
     * 反注册订阅者
     * @param topic 主题
     * @param subscriber 订阅者
     */
    void deregisterSubscriber(String topic, Subscriber<? extends JarbootEvent> subscriber);

    /**
     * 接收事件
     * @param topic 主题
     * @param event 事件
     */
    void receiveEvent(String topic, JarbootEvent event);

    /**
     * 创建任务生命周期事件主题
     * @param serviceName 服务名称
     * @param lifecycle 生命周期
     * @return 主题
     */
    default String createLifecycleTopic(String serviceName, TaskLifecycle lifecycle) {
        return StringUtils.concat(StringUtils.SLASH, serviceName, lifecycle.name());
    }
}
