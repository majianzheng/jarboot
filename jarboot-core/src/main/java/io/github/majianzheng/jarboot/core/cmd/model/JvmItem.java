package io.github.majianzheng.jarboot.core.cmd.model;

/**
 * @author majianzheng
 */
public class JvmItem {
    private String name;
    private Object value;
    private String desc;

    public JvmItem() {

    }

    public JvmItem(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
