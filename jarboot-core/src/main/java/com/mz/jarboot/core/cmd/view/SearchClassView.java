package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.core.cmd.model.SearchClassModel;
import com.mz.jarboot.core.utils.ClassUtils;
import com.mz.jarboot.text.util.RenderUtil;

/**
 * @author majianzheng
 */
public class SearchClassView implements ResultView<SearchClassModel> {
    @Override
    public String render(CommandSession session, SearchClassModel result) {
        StringBuilder sb = new StringBuilder();
        if (result.getMatchedClassLoaders() != null) {
            sb.append("Matched classloaders: \n");
            ClassLoaderView.drawClassLoaders(sb, result.getMatchedClassLoaders(), false, session.getCol());
            sb.append(StringUtils.LF);
            return sb.toString();
        }

        if (result.isDetailed()) {
            sb.append(RenderUtil.render(ClassUtils.renderClassInfo(result.getClassInfo(),
                    result.isWithField(), result.getExpand()), session.getCol()));
            sb.append("\n");
        } else if (result.getClassNames() != null) {
            for (String className : result.getClassNames()) {
                sb.append(className).append(StringUtils.LF);
            }
        }
        return sb.toString();
    }

}
