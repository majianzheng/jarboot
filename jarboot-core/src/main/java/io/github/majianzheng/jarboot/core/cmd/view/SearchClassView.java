package io.github.majianzheng.jarboot.core.cmd.view;

import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.core.cmd.model.SearchClassModel;
import io.github.majianzheng.jarboot.core.utils.ClassUtils;
import io.github.majianzheng.jarboot.text.util.RenderUtil;

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
