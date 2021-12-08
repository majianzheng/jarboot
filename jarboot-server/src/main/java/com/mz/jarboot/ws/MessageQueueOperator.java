package com.mz.jarboot.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.websocket.Session;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * WebSocket消息推送
 * @author majianzheng
 */
public class MessageQueueOperator {
    private static final Logger logger = LoggerFactory.getLogger(MessageQueueOperator.class);

    /** 消息堆积的最大个数 */
    private static final int MAX_MSG_QUEUE_SIZE = 16384;
    /** 待发送的消息队列 */
    private static final LinkedBlockingQueue<MessageSender> QUEUE = new LinkedBlockingQueue<>(MAX_MSG_QUEUE_SIZE);
    /** websocket会话 */
    private final Session session;
    
    public MessageQueueOperator(Session session) {
        this.session = session;
    }

    /**
     * 新消息投递
     * @param msg 消息
     */
    public void newMessage(String msg) {
        if (!QUEUE.offer(new MessageSender(session, msg))) {
            // 消息已满，丢弃
            logger.warn("消息过于频繁，未来的及处理，队列已满，将丢弃，消息：\n{}", msg);
        }
    }

    /**
     * 检查会话是否存活
     * @return 是否存活
     */
    public boolean isOpen() {
        return this.session.isOpen();
    }

    /**
     * 消费消息
     */
    public static void consumeMessage() {
        for (; ; ) {
            if (takeAndSend()) {
                break;
            }
        }
    }

    private static boolean takeAndSend() {
        try {
            final MessageSender sender = QUEUE.take();
            sender.sendText();
        } catch (InterruptedException e) {
            logger.info(e.getMessage(), e);
            Thread.currentThread().interrupt();
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }
}
