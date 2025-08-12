package io.github.majianzheng.jarboot;

import io.github.majianzheng.jarboot.api.AgentService;
import io.github.majianzheng.jarboot.api.JarbootFactory;
import io.github.majianzheng.jarboot.api.cmd.spi.CommandProcessor;
import io.github.majianzheng.jarboot.command.SpringBeanCommandProcessor;
import io.github.majianzheng.jarboot.command.SpringEnvCommandProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * @author majianzheng
 */
@ConditionalOnProperty(name = "spring.jarboot.enabled", matchIfMissing = true)
@EnableConfigurationProperties({ JarbootConfigProperties.class })
@SuppressWarnings({"java:S6830"})
public class JarbootAutoConfiguration {
    @Bean
    @ConditionalOnClass(name = Constants.AGENT_CLASS)
    public AgentService agentService() {
        return JarbootFactory.createAgentService();
    }

    @Bean
    public JarbootTemplate jarbootTemplate(JarbootConfigProperties properties) {
        return new JarbootTemplate(properties);
    }

    @Bean("spring.env")
    public CommandProcessor springEnv(Environment environment) {
        return new SpringEnvCommandProcessor(environment);
    }

    @Bean("spring.bean")
    public CommandProcessor springBean(ApplicationContext context) {
        return new SpringBeanCommandProcessor(context);
    }
}
