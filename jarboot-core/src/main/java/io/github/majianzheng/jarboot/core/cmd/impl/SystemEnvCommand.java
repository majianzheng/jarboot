package io.github.majianzheng.jarboot.core.cmd.impl;


import io.github.majianzheng.jarboot.api.cmd.annotation.Argument;
import io.github.majianzheng.jarboot.api.cmd.annotation.Description;
import io.github.majianzheng.jarboot.api.cmd.annotation.Name;
import io.github.majianzheng.jarboot.api.cmd.annotation.Summary;
import io.github.majianzheng.jarboot.core.cmd.AbstractCommand;
import io.github.majianzheng.jarboot.core.cmd.model.SystemEnvModel;
import io.github.majianzheng.jarboot.core.constant.CoreConstant;
import io.github.majianzheng.jarboot.common.utils.StringUtils;

/**
 * @author majianzheng
 */
@Name("sysenv")
@Summary("Display the system env.")
@Description(CoreConstant.EXAMPLE + "  sysenv\n" + "  sysenv USER\n" +
        CoreConstant.WIKI + CoreConstant.WIKI_HOME + "sysenv")
public class SystemEnvCommand extends AbstractCommand {

    private String envName;

    @Argument(index = 0, argName = "env-name", required = false)
    @Description("env name")
    public void setOptionName(String envName) {
        this.envName = envName;
    }

    @Override
    public boolean isRunning() {
        return session.isRunning();
    }

    @Override
    public void cancel() {
        session.cancel();
    }

    @Override
    public void run() {
        try {
            SystemEnvModel result = new SystemEnvModel();
            if (StringUtils.isBlank(envName)) {
                // show all system env
                result.putAll(System.getenv());
            } else {
                // view the specified system env
                String value = System.getenv(envName);
                result.put(envName, value);
            }
            session.appendResult(result);
            session.end();
        } catch (Exception t) {
            session.end(false, "Error during setting system env: " + t.getMessage());
        }
    }
}
