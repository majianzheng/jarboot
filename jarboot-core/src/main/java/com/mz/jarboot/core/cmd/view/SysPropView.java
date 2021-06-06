package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.core.basic.SingletonCoreFactory;
import com.mz.jarboot.core.cmd.model.SysPropModel;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * @author jianzhengma
 */
public class SysPropView implements ResultView<SysPropModel> {
    public String render(SysPropModel model) {
        TemplateEngine engine = SingletonCoreFactory.getInstance().createTemplateEngine();
        Context context = new Context();
        context.setVariable("props", model.getProps());
        return engine.process("template/SysPropView.html", context);
    }
}
