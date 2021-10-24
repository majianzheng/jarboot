package com.mz.jarboot.command;

import com.mz.jarboot.api.cmd.annotation.Argument;
import com.mz.jarboot.api.cmd.annotation.Description;
import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.annotation.Summary;
import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.api.cmd.spi.CommandProcessor;
import org.springframework.core.env.Environment;

import java.lang.instrument.Instrumentation;

/**
 * Get Spring Environment Command Processor
 * @author majianzheng
 */
@Name("spring.env")
@Summary("Get Spring Environment property")
@Description("\nEXAMPLES:\n spring.env server.port")
public class SpringEnvCommandProcessor implements CommandProcessor {

    private String propKey;
    private final Environment environment;

    public SpringEnvCommandProcessor(Environment environment) {
        this.environment = environment;
    }

    @Argument(argName = "env key", index = 0)
    @Description("Environment property key name.")
    public void setPropKey(String propKey) {
        this.propKey = propKey;
    }

    @Override
    public void postConstruct(Instrumentation inst, String server) {
        //do nothing
    }

    @Override
    public String process(CommandSession session, String[] args) {
        return environment.getProperty(this.propKey, "Not found.");
    }

    @Override
    public void afterProcess(String result, Throwable e) {
        this.propKey = null;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
