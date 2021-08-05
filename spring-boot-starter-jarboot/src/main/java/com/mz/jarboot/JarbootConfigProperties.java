package com.mz.jarboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author majianzheng
 */
@ConfigurationProperties(JarbootConfigProperties.PREFIX)
public class JarbootConfigProperties {

    /**
     * Prefix of {@link JarbootConfigProperties}.
     */
    public static final String PREFIX = "spring.jarboot";

    private boolean failedAutoExit = true;

    public boolean isFailedAutoExit() {
        return failedAutoExit;
    }

    public void setFailedAutoExit(boolean failedAutoExit) {
        this.failedAutoExit = failedAutoExit;
    }
}
