package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.core.cmd.model.SysPropModel;
import com.mz.jarboot.core.utils.HtmlRenderUtils;
import org.thymeleaf.context.Context;

/**
 * @author majianzheng
 */
public class SysPropView implements ResultView<SysPropModel> {
    @Override
    public String render(SysPropModel model) {
        Context context = new Context();
        context.setVariable("props", model.getProps());
        return HtmlRenderUtils.getInstance().processHtml("template/SysPropView.html", context);
    }
}
