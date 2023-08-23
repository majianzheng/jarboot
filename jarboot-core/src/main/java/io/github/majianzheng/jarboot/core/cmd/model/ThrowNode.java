package io.github.majianzheng.jarboot.core.cmd.model;

/**
 * Throw exception info node of TraceCommand
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class ThrowNode extends TraceNode {
    private String exception;
    private String message;
    private int lineNumber;

    public ThrowNode() {
        super("throw");
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
