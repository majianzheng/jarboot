package com.mz.jarboot.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.websocket.Session;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * WebSocket消息推送
 * @author majianzheng
 */
public class MessageQueueOperator {
    private static final Logger logger = LoggerFactory.getLogger(MessageQueueOperator.class);
    private static final int MAX_MSG_QUEUE_SIZE = 16384;

    private final Session session;
    private static final BlockingQueue<MessageSender> QUEUE = new ArrayBlockingQueue<>(MAX_MSG_QUEUE_SIZE);
    
    public MessageQueueOperator(Session session) {
        this.session = session;
    }
    
    public void newMessage(String msg) {
        if (!QUEUE.offer(new MessageSender(session, msg))) {
            // 消息已满，丢弃
            logger.warn("消息过于频繁，未来的及处理，队列已满，将丢弃，消息：\n{}", msg);
        }
    }

    public static BlockingQueue<MessageSender> getQueue() {
        return QUEUE;
    }
}
