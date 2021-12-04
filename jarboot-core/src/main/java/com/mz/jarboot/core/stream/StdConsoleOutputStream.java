package com.mz.jarboot.core.stream;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 标准输出流实现类
 * @author majianzheng
 */
@SuppressWarnings("all")
public class StdConsoleOutputStream extends OutputStream {
    /** 行结束标识 */
    private static final int LINE_BREAK = 10;
    /** 最小打印字符长度 */
    private static final int MIN_PRINT_UNIT = 12;
    /** IO刷新阈值 */
    private static final int  FLUSH_THRESHOLD = MIN_PRINT_UNIT * 128;
    /** buffer起始的无效索引 */
    private static final int NO_BUFFER_OFFSET = -1;
    /** 回车字符 */
    private static final byte CR = '\r';
    /** 退格键 */
    private static final byte BACKSPACE = '\b';
    /** IO 字符缓存 */
    private byte[] buffer = new byte[FLUSH_THRESHOLD];
    /** buffer当前索引位置 */
    private volatile int offset = NO_BUFFER_OFFSET;
    /** 退格的计数值 */
    private AtomicInteger backspaceNum = new AtomicInteger(0);
    /** 行处理接口 */
    private StdPrintHandler printLineHandler;
    /** 文本处理接口 */
    private StdPrintHandler printHandler;
    /** 退格处理接口 */
    private StdBackspaceHandler backspaceHandler;
    /** IO 唤醒接口 */
    private Runnable weakup;

    /**
     * 设置唤醒接口
     * @param weakup 唤醒接口
     */
    public StdConsoleOutputStream(Runnable weakup) {
        this.weakup = weakup;
    }

    /**
     * 设置行处理接口
     * @param printLineHandler 行处理接口
     */
    public void setPrintLineHandler(StdPrintHandler printLineHandler) {
        this.printLineHandler = printLineHandler;
    }

    /**
     * 设置文本处理接口
     * @param printHandler 文本处理接口
     */
    public void setPrintHandler(StdPrintHandler printHandler) {
        this.printHandler = printHandler;
    }

    /**
     * 设置退格处理接口
     * @param handler 退格处理接口
     */
    public void setBackspaceHandler(StdBackspaceHandler handler) {
        this.backspaceHandler = handler;
    }

    /**
     * 重写write
     * @param b byte字节
     */
    @Override
    public void write(int b) {
        if ((offset + 1) >= buffer.length) {
            if (CR == buffer[buffer.length - 1]) {
                offset = buffer.length - 2;
                this.print();
                offset = 0;
                buffer[0] = buffer[buffer.length - 1];
            } else {
                this.print();
            }
        }
        byte c = (byte)b;
        if (BACKSPACE == c) {
            this.backspaceNum.incrementAndGet();
            weakup.run();
            return;
        }
        //int的高24位是无效的，实际只用到8位
        buffer[++offset]  = c;
        if (LINE_BREAK == b) {
            this.println();
        } else {
            if (offset >= (FLUSH_THRESHOLD - 1)) {
                this.flush();
            } else {
                weakup.run();
            }
        }
    }

    /**
     * IO 刷新
     */
    @Override
    public void flush() {
        this.backspace();
        //打印
        this.print();
    }

    /**
     * 打印行
     */
    private void println() {
        String text = "";
        //一行，清空buffer，打印一行
        int len = offset > 1 && (CR == buffer[offset - 1]) ? offset - 1 : offset;
        if (len > 0) {
            text = new String(buffer, 0, len);
        }
        offset = NO_BUFFER_OFFSET;
        printLineHandler.handle(text);
    }

    /**
     * 打印文本
     */
    private void print() {
        //一行，清空buffer，打印一行
        if (offset > 0) {
            String text = new String(buffer, 0, offset + 1);
            offset = NO_BUFFER_OFFSET;
            printHandler.handle(text);
        }
    }

    /**
     * 退格
     */
    private void backspace() {
        this.backspaceHandler.handle(this.backspaceNum.getAndSet(0));
    }
}
