package io.github.majianzheng.jarboot.core.event;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;

/**
 * @author majianzheng
 */
public class StdoutAppendEvent implements JarbootEvent {
    private final String text;
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
