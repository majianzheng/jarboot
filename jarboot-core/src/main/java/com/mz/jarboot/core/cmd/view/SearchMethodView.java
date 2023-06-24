package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.core.cmd.model.MethodVO;
import com.mz.jarboot.core.cmd.model.SearchMethodModel;
import com.mz.jarboot.core.utils.ClassUtils;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.text.util.RenderUtil;

/**
 * render for SearchMethodCommand
 * @author majianzheng
 */
public class SearchMethodView implements ResultView<SearchMethodModel> {
    @Override
    public String render(CommandSession session, SearchMethodModel result) {
        StringBuilder sb = new StringBuilder();
        if (result.getMatchedClassLoaders() != null) {
            sb.append("Matched classloaders: \n");
            ClassLoaderView.drawClassLoaders(sb, result.getMatchedClassLoaders(), false, session.getCol());
            sb.append(StringUtils.LF);
            return sb.toString();
        }

        boolean detail = result.isDetail();
        MethodVO methodInfo = result.getMethodInfo();

        if (detail) {
            if (methodInfo.isConstructor()) {
                //render constructor
                sb.append(RenderUtil.render(ClassUtils.renderConstructor(methodInfo), session.getCol()));
            } else {
                //render method
                sb.append(RenderUtil.render(ClassUtils.renderMethod(methodInfo), session.getCol()));
            }
            sb.append(StringUtils.LF);
        } else {
            //java.util.List indexOf(Ljava/lang/Object;)I
            //className methodName+Descriptor
            sb
                    .append(methodInfo.getDeclaringClass())
                    .append(StringUtils.EMPTY)
                    .append(methodInfo.getMethodName())
                    .append(methodInfo.getDescriptor())
                    .append(StringUtils.LF);
        }
        return sb.toString();
    }
}
