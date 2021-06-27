package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.core.cmd.impl.ClassLoaderCommand;
import com.mz.jarboot.core.cmd.model.ClassDetailVO;
import com.mz.jarboot.core.cmd.model.ClassLoaderModel;
import com.mz.jarboot.core.cmd.model.ClassLoaderVO;
import com.mz.jarboot.core.cmd.model.ClassSetVO;
import com.mz.jarboot.core.cmd.view.element.Element;
import com.mz.jarboot.core.cmd.view.element.TableElement;
import com.mz.jarboot.core.cmd.view.element.TreeElement;
import com.mz.jarboot.core.utils.ClassUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author majianzheng
 */
public class ClassLoaderView implements ResultView<com.mz.jarboot.core.cmd.model.ClassLoaderModel> {

    @Override
    public String render(ClassLoaderModel result) {
        StringBuilder sb = new StringBuilder();
        if (result.getMatchedClassLoaders() != null) {
            sb.append("Matched classloaders: \n");
            drawClassLoaders(sb, result.getMatchedClassLoaders(), false);
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
            drawClassLoaders(sb, result.getClassLoaders(), result.getTree());
        }
        if (result.getClassLoaderStats() != null){
            drawClassLoaderStats(sb, result.getClassLoaderStats());
        }
        return sb.toString();
    }

    private void drawClassLoaderStats(StringBuilder process, Map<String, ClassLoaderCommand.ClassLoaderStat> classLoaderStats) {
        TableElement element = renderStat(classLoaderStats);
        process.append(element.toHtml());
    }

    private static TableElement renderStat(Map<String, ClassLoaderCommand.ClassLoaderStat> classLoaderStats) {
        TableElement table = new TableElement();
        table.row(true, "name", "numberOfInstances", "loadedCountTotal");
        for (Map.Entry<String, ClassLoaderCommand.ClassLoaderStat> entry : classLoaderStats.entrySet()) {
            table.row(entry.getKey(), "" + entry.getValue().getNumberOfInstance(), "" + entry.getValue().getLoadedCount());
        }
        return table;
    }

    public static void drawClassLoaders(StringBuilder process, Collection<ClassLoaderVO> classLoaders, boolean isTree) {
        Element element = isTree ? renderTree(classLoaders) : renderTable(classLoaders);
        process.append(element.toHtml());
    }

    private void drawClassLoaderUrls(StringBuilder process, List<String> urls) {
        process.append(renderClassLoaderUrls(urls));
        process.append(com.mz.jarboot.core.constant.CoreConstant.EMPTY_STRING);
    }

    private void drawLoadClass(StringBuilder process, ClassDetailVO loadClass) {
        process.append(ClassUtils.renderClassInfo(loadClass).toHtml()).append("\n");
    }

    private void drawAllClasses(StringBuilder process, ClassSetVO classSetVO) {
        process.append(renderClasses(classSetVO).toHtml());
        process.append("\n");
    }

    private void drawResources(StringBuilder process, List<String> resources) {
        TableElement table = new TableElement();
        for (String resource : resources) {
            table.row(resource);
        }
        process.append(table.toHtml() + "\n");
    }

    private TableElement renderClasses(ClassSetVO classSetVO) {
        TableElement table = new TableElement();
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

    // 统计所有的ClassLoader的信息
    private static TableElement renderTable(Collection<ClassLoaderVO> classLoaderInfos) {
        TableElement table = new TableElement();
        table.row("name", "loadedCount", "hash", "parent");
        for (ClassLoaderVO classLoaderVO : classLoaderInfos) {
            table.row(classLoaderVO.getName(), "" + classLoaderVO.getLoadedCount(), classLoaderVO.getHash(), classLoaderVO.getParent());
        }
        return table;
    }

    // 以树状列出ClassLoader的继承结构
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
