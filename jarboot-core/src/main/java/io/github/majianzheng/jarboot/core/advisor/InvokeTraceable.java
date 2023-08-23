package io.github.majianzheng.jarboot.core.advisor;

/**
 * 方法调用跟踪<br/>
 * 当一个方法内部调用另外一个方法时，会触发此跟踪方法
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
@SuppressWarnings("java:S112")
public interface InvokeTraceable {

    /**
     * 调用之前跟踪
     * @param classLoader 类加载器
     * @param tracingClassName  调用类名
     * @param tracingMethodName 调用方法名
     * @param tracingMethodDesc 调用方法描述
     * @param tracingLineNumber 执行调用行数
     * @throws Throwable 通知过程出错
     */
    void invokeBeforeTracing(
            ClassLoader classLoader,
            String tracingClassName,
            String tracingMethodName,
            String tracingMethodDesc,
            int tracingLineNumber) throws Throwable;

    /**
     * 抛异常后跟踪
     * @param classLoader 类加载器
     * @param tracingClassName  调用类名
     * @param tracingMethodName 调用方法名
     * @param tracingMethodDesc 调用方法描述
     * @param tracingLineNumber 执行调用行数
     * @throws Throwable 通知过程出错
     */
    void invokeThrowTracing(
            ClassLoader classLoader,
            String tracingClassName,
            String tracingMethodName,
            String tracingMethodDesc,
            int tracingLineNumber) throws Throwable;


    /**
     * 调用之后跟踪
     * @param classLoader 类加载器
     * @param tracingClassName  调用类名
     * @param tracingMethodName 调用方法名
     * @param tracingMethodDesc 调用方法描述
     * @param tracingLineNumber 执行调用行数
     * @throws Throwable 通知过程出错
     */
    void invokeAfterTracing(
            ClassLoader classLoader,
            String tracingClassName,
            String tracingMethodName,
            String tracingMethodDesc,
            int tracingLineNumber) throws Throwable;


}
