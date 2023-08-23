package io.github.majianzheng.jarboot.core.cmd.view;

import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.common.AnsiLog;
import io.github.majianzheng.jarboot.core.cmd.model.DumpClassVO;
import io.github.majianzheng.jarboot.core.utils.ClassUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.core.utils.TypeRenderUtils;
import io.github.majianzheng.jarboot.core.cmd.model.DumpClassModel;
import io.github.majianzheng.jarboot.text.ui.Element;
import io.github.majianzheng.jarboot.text.ui.TableElement;
import io.github.majianzheng.jarboot.text.util.RenderUtil;

import java.util.List;

/**
 * @author majianzheng
 */
public class DumpClassView implements ResultView<DumpClassModel> {
    private CommandSession session;
    @Override
    public String render(CommandSession session, DumpClassModel result) {
        this.session = session;
        StringBuilder sb = new StringBuilder();
        if (result.getMatchedClassLoaders() != null) {
            sb.append("Matched classloaders: \n");
            ClassLoaderView.drawClassLoaders(sb, result.getMatchedClassLoaders(), false, session.getCol());
            sb.append(StringUtils.LF);
            return sb.toString();
        }
        if (result.getDumpedClasses() != null) {
            drawDumpedClasses(sb, result.getDumpedClasses());
        } else if (result.getMatchedClasses() != null) {
            Element table = ClassUtils.renderMatchedClasses(result.getMatchedClasses());
            sb.append(RenderUtil.render(table)).append(StringUtils.LF);
        }
        return sb.toString();
    }

    private void drawDumpedClasses(StringBuilder process, List<DumpClassVO> list) {
        TableElement table = new TableElement();
        table.row(true, "HASHCODE", "CLASSLOADER", "LOCATION");

        for (DumpClassVO clazz : list) {
            table.row(AnsiLog.red(clazz.getClassLoaderHash()),
                    RenderUtil.render(TypeRenderUtils.drawClassLoader(clazz)),
                    AnsiLog.red(clazz.getLocation()));
        }

        process
                .append(RenderUtil.render(table, session.getCol()))
                .append(StringUtils.EMPTY);
    }
}
