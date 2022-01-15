package com.mz.jarboot.client.event;

import com.mz.jarboot.common.notify.DefaultPublisher;

/**
 * @author majianzheng
 */
public class ClientEventPublisher extends DefaultPublisher {
    /** 消息堆积的最大个数 */
    private static final int MAX_MSG_QUEUE_SIZE = 16384;

    public ClientEventPublisher() {
        super(MAX_MSG_QUEUE_SIZE, "jarboot.client.publisher");
    }
}
