package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.core.cmd.model.MethodVO;
import com.mz.jarboot.core.cmd.model.SearchMethodModel;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.utils.ClassUtils;

/**
 * render for SearchMethodCommand
 * @author majianzheng
 */
public class SearchMethodView implements ResultView<SearchMethodModel> {
    @Override
    public String render(SearchMethodModel result) {
        StringBuilder sb = new StringBuilder();
        if (result.getMatchedClassLoaders() != null) {
            sb.append("Matched classloaders: \n");
            ClassLoaderView.drawClassLoaders(sb, result.getMatchedClassLoaders(), false);
            sb.append(CoreConstant.BR);
            return sb.toString();
        }

        boolean detail = result.isDetail();
        MethodVO methodInfo = result.getMethodInfo();

        if (detail) {
            if (methodInfo.isConstructor()) {
                //render constructor
                sb.append(ClassUtils.renderConstructor(methodInfo).toHtml());
            } else {
                //render method
                sb.append(ClassUtils.renderMethod(methodInfo).toHtml());
            }
            sb.append(CoreConstant.BR);
        } else {
            //java.util.List indexOf(Ljava/lang/Object;)I
            //className methodName+Descriptor
            sb
                    .append(methodInfo.getDeclaringClass())
                    .append(CoreConstant.EMPTY_STRING)
                    .append(methodInfo.getMethodName())
                    .append(methodInfo.getDescriptor())
                    .append(CoreConstant.BR);
        }
        return sb.toString();
    }
}
