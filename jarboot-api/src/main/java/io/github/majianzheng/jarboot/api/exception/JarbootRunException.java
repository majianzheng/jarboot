package io.github.majianzheng.jarboot.api.exception;

/**
 * @author jianzhengma
 */
public class JarbootRunException extends RuntimeException {
    private static final long serialVersionUID = 3513491993982293262L;

    public JarbootRunException(String errMsg) {
        super(errMsg);
    }

    public JarbootRunException(Throwable throwable) {
        super(throwable);
    }

    public JarbootRunException(String errMsg, Throwable throwable) {
        super(errMsg, throwable);
    }
}
