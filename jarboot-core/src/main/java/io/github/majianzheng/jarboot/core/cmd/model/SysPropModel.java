package io.github.majianzheng.jarboot.core.cmd.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author majianzheng
 */
public class SysPropModel extends ResultModel {
    private Map<String, String> props = new HashMap<>(32);

    public Map<String, String> getProps() {
        return props;
    }

    public void addProp(String key, String value) {
        props.put(key, value);
    }

    public void setProps(Properties properties) {
        properties.forEach((k, v) -> props.put(k.toString(), v.toString()));
    }

    @Override
    public String getName() {
        return "sysprop";
    }
}
