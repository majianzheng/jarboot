package com.mz.jarboot.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.websocket.Session;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket消息推送
 * @author jianzhengma
 */
public class MessageQueueOperator implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MessageQueueOperator.class);
    private static final int MAX_MSG_QUEUE_SIZE = 256;
    private volatile boolean running = false;

    private final Session session;
    private final ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(MAX_MSG_QUEUE_SIZE);
    public MessageQueueOperator(Session session) {
        this.session = session;
    }
    public void newMessage(String msg) {
        if (!queue.offer(msg)) {
            // 消息已满，丢弃
            logger.warn("消息过于频繁，未来的及处理，队列已满，将丢弃，消息：\n{}", msg);
        }
    }

    public ArrayBlockingQueue<String> getQueue() {
        return queue;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning() {
        this.running = true;
    }

    @Override
    public void run() {
        int timeoutCount = 0;
        // 若等于2，则6秒内没有消息就会释放
        final int maxWaitCount = 2;
        try {
            for (; ; ) {
                if (timeoutCount > maxWaitCount || !session.isOpen()) {
                    // 连续多次超时没有消息时，或回话已关闭时，释放
                    break;
                }
                String msg = queue.poll(2, TimeUnit.SECONDS);
                if (null == msg) {
                    // 消息已经清空
                    ++timeoutCount;
                } else {
                    timeoutCount = 0;
                    sendText(msg);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn(e.getMessage(), e);
        }
        // 在这个间隙，有可能会丢消息的，但这些消息并不重要，可以允许一定程度但丢失
        running = false;
    }

    private void sendText(String text) {
        try {
            session.getBasicRemote().sendText(text);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }
}
