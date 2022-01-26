package com.mz.jarboot.core.utils;

import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.mz.jarboot.core.basic.WsClientFactory;
import com.mz.jarboot.core.stream.ResultStreamDistributor;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


/**
 * 自定义日志Appender，集中管控，统一记录
 * @author majianzheng
 */
public class StreamCrossLogAppender<E> extends UnsynchronizedAppenderBase<E> {
    /** 未在线时记录本地日志文件 */
    private static final String LOCAL_FILE_NAME = "jarboot-core.log";
    /** 日志文件最大的大小 10M，超过时清空重写 */
    private static final long MAX_FILE_SIZE = (1024 * 1024 * 10);
    protected Encoder<E> encoder;

    public Encoder<E> getEncoder() {
        return encoder;
    }

    public StreamCrossLogAppender() {
        this.setName(LOCAL_FILE_NAME);
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
            writeLocalLog(byteArray);
        }
    }

    private void writeLocalLog(byte[] byteArray) {
        final java.io.File logFile = getLocalLogFile();
        try {
            if (!logFile.exists() && !createLogFile(logFile)) {
                return;
            }
            FileUtils.writeByteArrayToFile(logFile, byteArray, logFile.length() < MAX_FILE_SIZE);
        } catch (Exception e) {
            //ignore
        }
    }

    private boolean createLogFile(java.io.File logFile) throws IOException {
        java.io.File logDir = logFile.getParentFile();
        if (!logDir.exists() && !logDir.mkdirs()) {
            return false;
        }
        return logFile.createNewFile();
    }

    private java.io.File getLocalLogFile() {
        return FileUtils.getFile(LogUtils.getLogDir(), LOCAL_FILE_NAME);
    }
}
