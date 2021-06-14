package com.mz.jarboot.core.cmd.view;

import com.alibaba.fastjson.JSON;
import com.mz.jarboot.core.cmd.model.DumpClassModel;
import com.mz.jarboot.core.cmd.model.DumpClassVO;
import com.mz.jarboot.core.utils.ClassUtils;

import java.util.List;

/**
 * @author majianzheng
 */
public class DumpClassView implements ResultView<com.mz.jarboot.core.cmd.model.DumpClassModel> {

    @Override
    public String render(com.mz.jarboot.core.cmd.model.DumpClassModel result) {
//        StringBuilder builder = new StringBuilder();
//        if (result.getMatchedClassLoaders() != null) {
//            builder.append("Matched classloaders: \n");
//            ClassLoaderView.drawClassLoaders(process, result.getMatchedClassLoaders(), false);
//            builder.append("\n");
//            return builder.toString();
//        }
//        if (result.getDumpedClasses() != null) {
//            drawDumpedClasses(process, result.getDumpedClasses());
//
//        } else if (result.getMatchedClasses() != null) {
//            Element table = ClassUtils.renderMatchedClasses(result.getMatchedClasses());
//            process.write(RenderUtil.render(table)).write("\n");
//        }
        return JSON.toJSONString(result);
    }

    @Override
    public boolean isJson() {
        return true;
    }
}
