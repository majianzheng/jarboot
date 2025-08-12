package io.github.majianzheng.jarboot.common.notify;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.common.utils.StringUtils;

/**
 * @author majianzheng
 */
public interface AbstractEventRegistry {
    @SuppressWarnings("java:S2386")
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
     * 创建主题
     * @param cls 事件类
     * @param args 参数
     * @return 主题
     */
    default String createTopic(Class<?> cls, String... args) {
        return createTopic(cls) + StringUtils.SLASH + StringUtils.concat(StringUtils.SLASH, args);
    }

    /**
     * 创建主题
     * @param cls 事件类
     * @return 主题
     */
    default String createTopic(Class<?> cls) {
        return cls.getName();
    }
}
