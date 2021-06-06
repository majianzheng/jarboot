package com.mz.jarboot.core.cmd.model;

import java.util.Properties;

public class SysPropModel extends ResultModel {
    private Properties props;

    public Properties getProps() {
        return props;
    }

    public void addProp(String key, Object value) {
        if (null == props) {
            props = new Properties();
        }
        props.put(key, value);
    }

    public void setProps(Properties props) {
        this.props = props;
    }

    @Override
    public String getName() {
        return "sysprop";
    }
}
