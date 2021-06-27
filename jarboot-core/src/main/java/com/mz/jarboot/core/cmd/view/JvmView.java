package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.core.cmd.model.JvmModel;
import com.mz.jarboot.core.utils.HtmlRenderUtils;
import org.thymeleaf.context.Context;

/**
 * @author jianzhengma
 */
public class JvmView implements ResultView<JvmModel> {
    public String render(JvmModel model) {
        Context context = new Context();
        context.setVariable("memoryInfo", model.getMemoryInfo());
        context.setVariable("memoryMgrInfo", model.getMemoryMgrInfo());
        context.setVariable("pendingFinalizationCount", model.getPendingFinalizationCount());
        context.setVariable("runtimeInfo", model.getRuntimeInfo());
        context.setVariable("classLoadingInfo", model.getClassLoadingInfo());
        context.setVariable("compilation", model.getCompilation());
        context.setVariable("operatingSystemInfo", model.getOperatingSystemInfo());
        context.setVariable("threadInfo", model.getThreadInfo());
        context.setVariable("garbageCollectorsInfo", model.getGarbageCollectorsInfo());
        context.setVariable("fileDescInfo", model.getFileDescInfo());
        return HtmlRenderUtils.getInstance().processHtml("template/JvmView.html", context);
    }
}
