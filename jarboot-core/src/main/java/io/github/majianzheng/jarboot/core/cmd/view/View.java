package io.github.majianzheng.jarboot.core.cmd.view;

/**
 * 命令行控件<br/>
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public interface View {

    /**
     * 输出外观
     * @return 渲染结果
     */
    String draw();

}