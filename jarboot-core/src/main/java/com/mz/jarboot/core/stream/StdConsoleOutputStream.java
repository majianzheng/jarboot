package com.mz.jarboot.core.stream;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author majianzheng
 */
@SuppressWarnings("all")
public class StdConsoleOutputStream extends OutputStream {
    private static final int LINE_BREAK = 10;
    private static final int MIN_PRINT_UNIT = 12;
    private static final int  FLUSH_THRESHOLD = MIN_PRINT_UNIT * 128;
    private static final int NO_BUFFER_OFFSET = -1;
    private static final byte CR = '\r';
    private static final byte BACKSPACE = '\b';
    private byte[] buffer = new byte[FLUSH_THRESHOLD];
    private volatile int offset = NO_BUFFER_OFFSET;
    private AtomicInteger backspaceNum = new AtomicInteger(0);
    private StdPrintHandler printLineHandler;
    private StdPrintHandler printHandler;
    private StdBackspaceHandler backspaceHandler;

    public void setPrintLineHandler(StdPrintHandler printLineHandler) {
        this.printLineHandler = printLineHandler;
    }

    public void setPrintHandler(StdPrintHandler printHandler) {
        this.printHandler = printHandler;
    }
    
    public void setBackspaceHandler(StdBackspaceHandler handler) {
        this.backspaceHandler = handler;
    }

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
            return;
        }
        //int的高24位是无效的，实际只用到8位
        buffer[++offset]  = c;
        if (LINE_BREAK == b) {
            this.println();
        } else {
            if (offset >= (FLUSH_THRESHOLD - 1)) {
                this.flush();
            }
        }
    }
    @Override
    public void flush() {
        this.backspace();
        //打印
        this.print();
    }

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

    private void print() {
        //一行，清空buffer，打印一行
        if (offset > 0) {
            String text = new String(buffer, 0, offset + 1);
            offset = NO_BUFFER_OFFSET;
            printHandler.handle(text);
        }
    }

    private void backspace() {
        this.backspaceHandler.handle(this.backspaceNum.getAndSet(0));
    }
}
