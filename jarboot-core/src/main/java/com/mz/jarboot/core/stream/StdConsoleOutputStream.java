package com.mz.jarboot.core.stream;

import java.io.OutputStream;

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
    private StringBuilder sb = new StringBuilder(2048);
    private final byte[] buffer = new byte[MIN_PRINT_UNIT * 100];
    private int offset = NO_BUFFER_OFFSET;
    private StdPrintHandler printLineHandler;
    private StdPrintHandler printHandler;

    public StdPrintHandler getPrintLineHandler() {
        return printLineHandler;
    }

    public void setPrintLineHandler(StdPrintHandler printLineHandler) {
        this.printLineHandler = printLineHandler;
    }

    public StdPrintHandler getPrintHandler() {
        return printHandler;
    }

    public void setPrintHandler(StdPrintHandler printHandler) {
        this.printHandler = printHandler;
    }

    @Override
    public void write(int b) {
        if ((++offset) >= buffer.length) {
            if (CR == buffer[buffer.length - 1]) {
                offset = 1;
                sb.append(new String(buffer, 0, buffer.length - 1));
                buffer[0] = buffer[buffer.length - 1];
            } else {
                offset = 0;
                sb.append(new String(buffer));
            }
        }
        //int的高24位是无效的，实际只用到8位
        buffer[offset]  = (byte)b;
        if (LINE_BREAK == b) {
            if (null != printLineHandler) {
                printLineHandler.handle(getAndReset());
            }
        } else {
            if (sb.length() + offset > FLUSH_THRESHOLD) {
                flush();
            }
        }
    }
    @Override
    public void flush() {
        //打印
        if (NO_BUFFER_OFFSET != offset && null != printHandler) {
            printHandler.handle(getAndReset());
        }
    }

    private String getAndReset() {
        //一行，清空buffer，打印一行
        int len = offset > 1 && (CR == buffer[offset - 1]) ? offset - 1 : offset;
        if (len > 0) {
            sb.append(new String(buffer, 0, len));
        }
        offset = NO_BUFFER_OFFSET;
        String text = sb.toString();
        sb = new StringBuilder(2048);
        return text;
    }
}
