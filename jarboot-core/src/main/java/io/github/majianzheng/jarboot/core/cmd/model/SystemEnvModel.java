package io.github.majianzheng.jarboot.core.cmd.model;

import java.util.HashMap;
import java.util.Map;

/**
 * sysenv KV Result
 * @author majianzheng
 */
public class SystemEnvModel extends ResultModel {

    private Map<String, String> env = new HashMap<>();

    public SystemEnvModel() {
    }

    public SystemEnvModel(Map<String, String> env) {
        this.putAll(env);
    }

    public SystemEnvModel(String name, String value) {
        this.put(name, value);
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public String put(String key, String value) {
        return env.put(key, value);
    }

    public void putAll(Map<String, String> m) {
        env.putAll(m);
    }

    @Override
    public String getName() {
        return "sysenv";
    }
}
