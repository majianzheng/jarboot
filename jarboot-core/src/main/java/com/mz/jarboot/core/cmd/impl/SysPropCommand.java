package com.mz.jarboot.core.cmd.impl;

import com.mz.jarboot.core.cmd.Command;
import com.mz.jarboot.core.cmd.annotation.Argument;
import com.mz.jarboot.core.cmd.annotation.Description;
import com.mz.jarboot.core.cmd.model.SysPropModel;
import com.mz.jarboot.core.utils.StringUtils;

/**
 * show the jvm detail
 * @author jianzhengma
 */
public class SysPropCommand extends Command {
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
    public boolean isRunning() {
        return null != session && session.isRunning();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void cancel() {
        //do nothing
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
