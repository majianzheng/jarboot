package io.github.majianzheng.jarboot.core.cmd.model;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class ChangeResultVO {
    private String name;
    private Object beforeValue;
    private Object afterValue;

    public ChangeResultVO() {
        //do nothing
    }

    public ChangeResultVO(String name, Object beforeValue, Object afterValue) {
        this.name = name;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getBeforeValue() {
        return beforeValue;
    }

    public void setBeforeValue(Object beforeValue) {
        this.beforeValue = beforeValue;
    }

    public Object getAfterValue() {
        return afterValue;
    }

    public void setAfterValue(Object afterValue) {
        this.afterValue = afterValue;
    }
}

