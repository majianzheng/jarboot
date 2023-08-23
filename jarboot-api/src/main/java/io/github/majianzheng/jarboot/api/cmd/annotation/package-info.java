/**
 * <h2>命令构建注解</h2>
 * 用法：<br>
 * <code>
 *     //Command SPI类实现：<br>
 *     //使用{@link io.github.majianzheng.jarboot.api.cmd.annotation.Name}定义命令名<br>
 * -   @Name("demo")<br>
 * -   class DemoCommand implements CommandProcessor<br>
 * <br>
 * -   //使用{@link io.github.majianzheng.jarboot.api.cmd.annotation.Argument}定义命令的参数输入<br>
 * -   @Argument(argName = "name", index = 0, required = false)<br>
 * -   @Description("Argument description")<br>
 * -   void setArg(String arg)<br>
 * <br>
 * -   //使用{@link io.github.majianzheng.jarboot.api.cmd.annotation.Option}定义命令Option参数<br>
 * -   //Option参数格式为： -参数名 参数值（如：-c DemoClass）<br>
 * -   @Option(shortName = "c", longName = "code")<br>
 * -   @Description("Option description")<br>
 * -   void setSomeValue(String value)<br>
 * </code>
 */
package io.github.majianzheng.jarboot.api.cmd.annotation;
