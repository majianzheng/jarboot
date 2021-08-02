package com.mz.jarboot.api.cmd.spi;

import com.mz.jarboot.api.cmd.session.CommandSession;

import java.lang.instrument.Instrumentation;

/**
 * <h2>自定义命令SPI扩展</h2>
 * 当内置命令中不存在时，将会尝试使用SPI寻找自定义的命令处理方法<br>
 * <span style="color: yellow">注：</span>如果内置的命令已存在同名的命令，则会忽略自定义的SPI命令<br>
 * 使用类注解{@link com.mz.jarboot.api.cmd.annotation.Name}定义命令的名字，在Spring应用中若没有使用该注解，则
 * 会使用Bean的名字作为命令的名字<br>
 * 使用{@link com.mz.jarboot.api.cmd.annotation}包中的方法注解，定义命令传入的参数<br>
 * {@link com.mz.jarboot.api.cmd.annotation.Argument} 参数<br>
 * {@link com.mz.jarboot.api.cmd.annotation.Option} Option参数或flag<br>
 * {@link com.mz.jarboot.api.cmd.annotation.DefaultValue} 默认值<br>
 * {@link com.mz.jarboot.api.cmd.annotation.Description} 参数说明<br>
 * @author jianzhengma
 */
public interface CommandProcessor {
    /**
     * <h3>命令实例构建完成后，执行命令之前</h3>
     * @param inst instrumentation
     * @param server 服务名
     */
    default void postConstruct(Instrumentation inst, String server) {
        //ignore
    }

    /**
     * <h3>自定义命令行处理</h3>
     * @param session 命令名称
     * @param args    命令参数
     * @return 命令执行结果
     */
    String process(CommandSession session, String[] args);
}
