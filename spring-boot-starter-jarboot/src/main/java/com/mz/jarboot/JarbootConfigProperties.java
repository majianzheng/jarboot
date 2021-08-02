package com.mz.jarboot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

/**
 * @author majianzheng
 */
@ConfigurationProperties(JarbootConfigProperties.PREFIX)
public class JarbootConfigProperties {

    /**
     * Prefix of {@link JarbootConfigProperties}.
     */
    public static final String PREFIX = "spring.jarboot";

    @Autowired
    @JsonIgnore
    private Environment environment;

    private String username = "jarboot";

    private String password = "jarboot";

    private boolean failedAutoExit = true;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isFailedAutoExit() {
        return failedAutoExit;
    }

    public void setFailedAutoExit(boolean failedAutoExit) {
        this.failedAutoExit = failedAutoExit;
    }
}
