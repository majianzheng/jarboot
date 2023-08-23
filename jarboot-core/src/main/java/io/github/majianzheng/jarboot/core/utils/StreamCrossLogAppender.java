package io.github.majianzheng.jarboot.core.utils;

import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import io.github.majianzheng.jarboot.core.basic.WsClientFactory;
import io.github.majianzheng.jarboot.core.stream.ResultStreamDistributor;

import java.nio.charset.StandardCharsets;


/**
 * 自定义日志Appender，集中管控，统一记录
 * @author majianzheng
 */
public class StreamCrossLogAppender<E> extends UnsynchronizedAppenderBase<E> {
    protected Encoder<E> encoder;

    public Encoder<E> getEncoder() {
        return encoder;
    }

    public StreamCrossLogAppender() {
        this.setName(LogUtils.LOCAL_FILE_NAME);
    }

    public void setEncoder(Encoder<E> encoder) {
        this.encoder = encoder;
    }

    @Override
    protected void append(E event) {
        if (!isStarted()) {
            return;
        }
        if (event instanceof DeferredProcessingAware) {
            ((DeferredProcessingAware) event).prepareForDeferredProcessing();
        }
        byte[] byteArray = this.encoder.encode(event);
        if (WsClientFactory.getInstance().isOnline()) {
            String msg = new String(byteArray, StandardCharsets.UTF_8);
            ResultStreamDistributor.getInstance().log(msg);
        } else {
            LogUtils.writeLocalLog(byteArray);
        }
    }
}
