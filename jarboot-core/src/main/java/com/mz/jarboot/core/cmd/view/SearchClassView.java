package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.core.cmd.model.SearchClassModel;
import com.mz.jarboot.core.utils.ClassUtils;

/**
 * @author majianzheng
 */
public class SearchClassView implements ResultView<SearchClassModel> {
    @Override
    public String render(SearchClassModel result) {
        StringBuilder sb = new StringBuilder();
        if (result.getMatchedClassLoaders() != null) {
            sb.append("Matched classloaders: \n");
            ClassLoaderView.drawClassLoaders(sb, result.getMatchedClassLoaders(), false);
            sb.append("\n");
            return sb.toString();
        }

        if (result.isDetailed()) {
            sb.append(ClassUtils.renderClassInfo(result.getClassInfo(),
                    result.isWithField(), result.getExpand()).toHtml());
            sb.append("\n");
        } else if (result.getClassNames() != null) {
            for (String className : result.getClassNames()) {
                sb.append(className).append("\n");
            }
        }
        return sb.toString();
    }

}
