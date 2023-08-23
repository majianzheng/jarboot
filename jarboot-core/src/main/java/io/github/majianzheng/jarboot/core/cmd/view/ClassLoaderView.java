package io.github.majianzheng.jarboot.core.cmd.view;

import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.common.AnsiLog;
import io.github.majianzheng.jarboot.core.cmd.impl.ClassLoaderCommand;
import io.github.majianzheng.jarboot.core.cmd.model.ClassDetailVO;
import io.github.majianzheng.jarboot.core.cmd.model.ClassLoaderModel;
import io.github.majianzheng.jarboot.core.cmd.model.ClassLoaderVO;
import io.github.majianzheng.jarboot.core.cmd.model.ClassSetVO;
import io.github.majianzheng.jarboot.core.utils.ClassUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.text.ui.Element;
import io.github.majianzheng.jarboot.text.ui.TableElement;
import io.github.majianzheng.jarboot.text.ui.TreeElement;
import io.github.majianzheng.jarboot.text.util.RenderUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author majianzheng
 */
public class ClassLoaderView implements ResultView<ClassLoaderModel> {
    private CommandSession session;
    @Override
    public String render(CommandSession session, ClassLoaderModel result) {
        this.session = session;
        StringBuilder sb = new StringBuilder();
        if (result.getMatchedClassLoaders() != null) {
            sb.append("Matched classloaders: \n");
            drawClassLoaders(sb, result.getMatchedClassLoaders(), false, session.getCol());
            sb.append("\n");
            return sb.toString();
        }
        if (result.getClassSet() != null) {
            drawAllClasses(sb, result.getClassSet());
        }
        if (result.getResources() != null) {
            drawResources(sb, result.getResources());
        }
        if (result.getLoadClass() != null) {
            drawLoadClass(sb, result.getLoadClass());
        }
        if (result.getUrls() != null) {
            drawClassLoaderUrls(sb, result.getUrls());
        }
        if (result.getClassLoaders() != null){
            drawClassLoaders(sb, result.getClassLoaders(), result.getTree(), session.getCol());
        }
        if (result.getClassLoaderStats() != null){
            drawClassLoaderStats(sb, result.getClassLoaderStats());
        }
        return sb.toString();
    }

    private void drawClassLoaderStats(StringBuilder process, Map<String, ClassLoaderCommand.ClassLoaderStat> classLoaderStats) {
        TableElement element = renderStat(classLoaderStats);
        process.append(RenderUtil.render(element, session.getCol()));
    }

    private TableElement renderStat(Map<String, ClassLoaderCommand.ClassLoaderStat> classLoaderStats) {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.row(true, "name", "numberOfInstances", "loadedCountTotal");
        for (Map.Entry<String, ClassLoaderCommand.ClassLoaderStat> entry : classLoaderStats.entrySet()) {
            table.row(entry.getKey(), "" + entry.getValue().getNumberOfInstance(), "" + entry.getValue().getLoadedCount());
        }
        return table;
    }

    public static void drawClassLoaders(StringBuilder process, Collection<ClassLoaderVO> classLoaders, boolean isTree, int col) {
        Element element = isTree ? renderTree(classLoaders) : renderTable(classLoaders);
        process.append(RenderUtil.render(element, col));
    }

    private void drawClassLoaderUrls(StringBuilder process, List<String> urls) {
        process.append(renderClassLoaderUrls(urls));
        process.append(StringUtils.EMPTY);
    }

    private void drawLoadClass(StringBuilder process, ClassDetailVO loadClass) {
        process.append(RenderUtil.render(ClassUtils.renderClassInfo(loadClass), session.getCol())).append(StringUtils.LF);
    }

    private void drawAllClasses(StringBuilder process, ClassSetVO classSetVO) {
        process.append(RenderUtil.render(renderClasses(classSetVO), session.getCol()));
        process.append("\n");
    }

    private void drawResources(StringBuilder process, List<String> resources) {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        for (String resource : resources) {
            table.row(resource);
        }
        process.append(RenderUtil.render(table, session.getCol()) + StringUtils.LF);
    }

    private TableElement renderClasses(ClassSetVO classSetVO) {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        if (classSetVO.getSegment() == 0) {
            table.row("hash:" + classSetVO.getClassloader().getHash() + ", " + classSetVO.getClassloader().getName());
        }
        for (String className : classSetVO.getClasses()) {
            table.row(className);
        }
        return table;
    }

    private static String renderClassLoaderUrls(List<String> urls) {
        StringBuilder sb = new StringBuilder();
        for (String url : urls) {
            sb.append(url).append("\n");
        }
        return sb.toString();
    }

    /**
     * 统计所有的ClassLoader的信息
     * @param classLoaderInfos 类加载信息
     * @return 表
     */
    private static TableElement renderTable(Collection<ClassLoaderVO> classLoaderInfos) {
        TableElement table = new TableElement().rightCellPadding(1).leftCellPadding(1);
        table.row(
                AnsiLog.bold("name"),
                AnsiLog.bold("loadedCount"),
                AnsiLog.bold("hash"),
                AnsiLog.bold("parent"));
        for (ClassLoaderVO classLoaderVO : classLoaderInfos) {
            table.row(classLoaderVO.getName(), "" + classLoaderVO.getLoadedCount(), classLoaderVO.getHash(), classLoaderVO.getParent());
        }
        return table;
    }

    /**
     * 以树状列出ClassLoader的继承结构
     * @param classLoaderInfos 类加载信息
     * @return 树
     */
    private static Element renderTree(Collection<ClassLoaderVO> classLoaderInfos) {
        TreeElement root = new TreeElement();
        for (ClassLoaderVO classLoader : classLoaderInfos) {
            TreeElement child = new TreeElement(classLoader.getName());
            root.addChild(child);
            renderSubtree(child, classLoader);
        }
        return root;
    }

    private static void renderSubtree(TreeElement parent, ClassLoaderVO parentClassLoader) {
        if (parentClassLoader.getChildren() == null){
            return;
        }
        for (ClassLoaderVO childClassLoader : parentClassLoader.getChildren()) {
            TreeElement child = new TreeElement(childClassLoader.getName());
            parent.addChild(child);
            renderSubtree(child, childClassLoader);
        }
    }
}
