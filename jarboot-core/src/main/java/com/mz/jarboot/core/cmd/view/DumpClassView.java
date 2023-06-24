package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.common.AnsiLog;
import com.mz.jarboot.core.cmd.model.DumpClassVO;
import com.mz.jarboot.core.utils.ClassUtils;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.core.utils.TypeRenderUtils;
import com.mz.jarboot.text.ui.Element;
import com.mz.jarboot.text.ui.TableElement;
import com.mz.jarboot.text.util.RenderUtil;

import java.util.List;

/**
 * @author majianzheng
 */
public class DumpClassView implements ResultView<com.mz.jarboot.core.cmd.model.DumpClassModel> {
    private CommandSession session;
    @Override
    public String render(CommandSession session, com.mz.jarboot.core.cmd.model.DumpClassModel result) {
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
