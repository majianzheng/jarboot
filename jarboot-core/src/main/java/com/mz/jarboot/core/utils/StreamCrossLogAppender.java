package com.mz.jarboot.core.utils;

import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.ResponseType;
import com.mz.jarboot.core.stream.ResultStreamDistributor;

import java.nio.charset.StandardCharsets;

import static ch.qos.logback.core.CoreConstants.CODES_URL;

/**
 * 自定义日志Appender，集中管控，统一记录
 * @author majianzheng
 */
public class StreamCrossLogAppender<E> extends UnsynchronizedAppenderBase<E> {
    protected Encoder<E> encoder;

    public void setLayout(Layout<E> layout) {
        addWarn("This appender no longer admits a layout as a sub-component, set an encoder instead.");
        addWarn("To ensure compatibility, wrapping your layout in LayoutWrappingEncoder.");
        addWarn("See also " + CODES_URL + "#layoutInsteadOfEncoder for details");
        LayoutWrappingEncoder<E> lwe = new LayoutWrappingEncoder<>();
        lwe.setLayout(layout);
        lwe.setContext(context);
        this.encoder = lwe;
    }

    public Encoder<E> getEncoder() {
        return encoder;
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
        String msg = new String(byteArray, StandardCharsets.UTF_8);
        CommandResponse resp = new CommandResponse();
        resp.setSuccess(true);
        resp.setResponseType(ResponseType.LOG_APPENDER);
        resp.setBody(msg);
        resp.setSessionId(CommandConst.SESSION_COMMON);
        ResultStreamDistributor.write(resp);
    }
}
