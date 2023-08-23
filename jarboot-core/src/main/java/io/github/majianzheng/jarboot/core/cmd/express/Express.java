package io.github.majianzheng.jarboot.core.cmd.express;

/**
 * 表达式
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public interface Express {

    /**
     * 根据表达式获取值
     *
     * @param express 表达式
     * @return 表达式运算后的值
     * @throws ExpressException 表达式运算出错
     */
    Object get(String express) throws ExpressException;

    /**
     * 根据表达式判断是与否
     *
     * @param express 表达式
     * @return 表达式运算后的布尔值
     * @throws ExpressException 表达式运算出错
     */
    boolean is(String express) throws ExpressException;

    /**
     * 绑定对象
     *
     * @param object 待绑定对象
     * @return this
     */
    Express bind(Object object);

    /**
     * 绑定变量
     *
     * @param name  变量名
     * @param value 变量值
     * @return this
     */
    Express bind(String name, Object value);

    /**
     * 重置整个表达式
     *
     * @return this
     */
    Express reset();


}
