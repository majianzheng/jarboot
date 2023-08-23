package io.github.majianzheng.jarboot.core.cmd.impl;

import io.github.majianzheng.jarboot.api.cmd.annotation.Name;
import io.github.majianzheng.jarboot.api.cmd.annotation.Summary;
import io.github.majianzheng.jarboot.core.cmd.AbstractCommand;
import io.github.majianzheng.jarboot.api.cmd.annotation.Argument;
import io.github.majianzheng.jarboot.api.cmd.annotation.Description;
import io.github.majianzheng.jarboot.core.cmd.model.SysPropModel;
import io.github.majianzheng.jarboot.core.constant.CoreConstant;
import io.github.majianzheng.jarboot.common.utils.StringUtils;

/**
 * show the system property
 * @author majianzheng
 */
@Name("sysprop")
@Summary("Display, and change the system properties.")
@Description(CoreConstant.EXAMPLE + "  sysprop\n"+ "  sysprop file.encoding\n" + "  sysprop production.mode true\n" +
        CoreConstant.WIKI + CoreConstant.WIKI_HOME + "sysprop")
public class SysPropCommand extends AbstractCommand {
    private SysPropModel model = new SysPropModel();
    private String propertyName;
    private String propertyValue;

    @Argument(index = 0, argName = "property-name", required = false)
    @Description("property name")
    public void setOptionName(String propertyName) {
        this.propertyName = propertyName;
    }

    @Argument(index = 1, argName = "property-value", required = false)
    @Description("property value")
    public void setOptionValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void run() {
        if (StringUtils.isBlank(propertyName) && StringUtils.isBlank(propertyValue)) {
            // show all system properties
            model.setProps(System.getProperties());
            session.appendResult(model);
        } else if (StringUtils.isBlank(propertyValue)) {
            // view the specified system property
            String value = System.getProperty(propertyName);
            if (value == null) {
                session.end(false, "There is no property with the key " + propertyName);
                return;
            } else {
                model.addProp(propertyName, value);
                session.appendResult(model);
            }
        } else {
            // change system property
            System.setProperty(propertyName, propertyValue);
            session.console("Successfully changed the system property.");
            model.addProp(propertyName, System.getProperty(propertyName));
            session.appendResult(model);
        }
        session.end();
    }
}
