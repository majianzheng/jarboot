package java.jarboot;

/**
 * SpyAPI
 * @author majianzheng
 * 以下代码基于开源项目Arthas修改
 */
@SuppressWarnings("all")
public class SpyAPI {
    public static final AbstractSpy NOP_SPY = new NopSpy();
    private static volatile AbstractSpy spyInstance = NOP_SPY;

    public static volatile boolean initialized;

    public static AbstractSpy getSpy() {
        return spyInstance;
    }

    public static void setSpy(AbstractSpy spy) {
        spyInstance = spy;
    }

    public static void setNopSpy() {
        setSpy(NOP_SPY);
    }

    public static boolean isNopSpy() {
        return NOP_SPY == spyInstance;
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInited() {
        return initialized;
    }

    public static void destroy() {
        setNopSpy();
        initialized = false;
    }

    public static void atEnter(Class<?> clazz, String methodInfo, Object target, Object[] args) {
        spyInstance.atEnter(clazz, methodInfo, target, args);
    }

    public static void atExit(Class<?> clazz, String methodInfo, Object target, Object[] args,
            Object returnObject) {
        spyInstance.atExit(clazz, methodInfo, target, args, returnObject);
    }

    public static void atExceptionExit(Class<?> clazz, String methodInfo, Object target,
            Object[] args, Throwable throwable) {
        spyInstance.atExceptionExit(clazz, methodInfo, target, args, throwable);
    }

    public static void atBeforeInvoke(Class<?> clazz, String invokeInfo, Object target) {
        spyInstance.atBeforeInvoke(clazz, invokeInfo, target);
    }

    public static void atAfterInvoke(Class<?> clazz, String invokeInfo, Object target) {
        spyInstance.atAfterInvoke(clazz, invokeInfo, target);
    }

    public static void atInvokeException(Class<?> clazz, String invokeInfo, Object target, Throwable throwable) {
        spyInstance.atInvokeException(clazz, invokeInfo, target, throwable);
    }

    public static abstract class AbstractSpy { //NOSONAR
        public abstract void atEnter(Class<?> clazz, String methodInfo, Object target,
                Object[] args);

        public abstract void atExit(Class<?> clazz, String methodInfo, Object target, Object[] args,
                Object returnObject);

        public abstract void atExceptionExit(Class<?> clazz, String methodInfo, Object target,
                Object[] args, Throwable throwable);

        public abstract void atBeforeInvoke(Class<?> clazz, String invokeInfo, Object target);

        public abstract void atAfterInvoke(Class<?> clazz, String invokeInfo, Object target);

        public abstract void atInvokeException(Class<?> clazz, String invokeInfo, Object target, Throwable throwable);
    }

    static class NopSpy extends AbstractSpy {

        @Override
        public void atEnter(Class<?> clazz, String methodInfo, Object target, Object[] args) {
            //NOSONAR
        }

        @Override
        public void atExit(Class<?> clazz, String methodInfo, Object target, Object[] args,
                Object returnObject) {
            //NOSONAR
        }

        @Override
        public void atExceptionExit(Class<?> clazz, String methodInfo, Object target, Object[] args,
                Throwable throwable) {
            //NOSONAR
        }

        @Override
        public void atBeforeInvoke(Class<?> clazz, String invokeInfo, Object target) {
            //NOSONAR
        }

        @Override
        public void atAfterInvoke(Class<?> clazz, String invokeInfo, Object target) {
            //NOSONAR
        }

        @Override
        public void atInvokeException(Class<?> clazz, String invokeInfo, Object target, Throwable throwable) {
            //NOSONAR
        }

    }
}
