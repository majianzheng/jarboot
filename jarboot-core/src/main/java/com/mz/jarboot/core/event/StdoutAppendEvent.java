package com.mz.jarboot.core.event;

import com.mz.jarboot.api.event.JarbootEvent;

/**
 * @author majianzheng
 */
public class StdoutAppendEvent implements JarbootEvent {
    private String text;
    public StdoutAppendEvent(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return "AppendStdoutFileEvent{" +
                "text='" + text + '\'' +
                '}';
    }
}
