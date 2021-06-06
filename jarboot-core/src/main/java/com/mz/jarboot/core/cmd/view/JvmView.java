package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.core.basic.SingletonCoreFactory;
import com.mz.jarboot.core.cmd.model.JvmModel;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * @author jianzhengma
 */
public class JvmView implements ResultView<JvmModel> {
    public String render(JvmModel model) {
        TemplateEngine engine = SingletonCoreFactory.getInstance().createTemplateEngine();
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
        return engine.process("template/JvmView.html", context);
    }
}
