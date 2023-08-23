package io.github.majianzheng.jarboot.api.cmd.spi;

import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.api.cmd.annotation.*;

import java.lang.instrument.Instrumentation;

/**
 * <h2>自定义命令SPI扩展</h2>
 * 当内置命令中不存在时，将会尝试使用SPI寻找自定义的命令处理方法<br>
 * <span style="color: yellow">注：</span>如果内置的命令已存在同名的命令，则会忽略自定义的SPI命令<br>
 * 使用类注解{@link Name}定义命令的名字，在Spring应用中若没有使用该注解，则
 * 会使用Bean的名字作为命令的名字<br>
 * 使用{@link io.github.majianzheng.jarboot.api.cmd.annotation}包中的方法注解，定义命令传入的参数<br>
 * {@link Argument} 参数<br>
 * {@link Option} Option参数或flag<br>
 * {@link DefaultValue} 默认值<br>
 * {@link Description} 参数说明<br>
 * @author jianzhengma
 */
public interface CommandProcessor {
    /**
     * <h3>命令实例构建完成后，执行命令之前</h3>
     * @param inst instrumentation
     * @param service 服务名
     */
    default void postConstruct(Instrumentation inst, String service) {
        //ignore
    }

    /**
     * <h3>自定义命令行处理</h3>
     * @param session 命令名称
     * @param args    命令参数
     * @return 命令执行结果
     */
    String process(CommandSession session, String[] args);

    /**
     * <h3>命令执行后</h3>
     * 可用于单例模式下的参数的重置操作
     * @param result 执行结果
     * @param e 失败时抛出的异常,执行成功为null
     */
    default void afterProcess(String result, Throwable e) {
        //ignore
    }
    
    /**
     * 取消执行
     */
    default void onCancel() {
        //ignore
    }

    /**
     * 是否为单例，若设定为单例需要注意线程安全和参数重置，默认均是多实例<br>
     * 内置命令、shell插件中命令均采用了多实例的模式
     * @return 是否单例
     */
    default boolean isSingleton() {
        return false;
    }
}
