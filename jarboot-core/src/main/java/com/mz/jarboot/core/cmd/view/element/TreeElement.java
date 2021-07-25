package com.mz.jarboot.core.cmd.view.element;

import com.mz.jarboot.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 树渲染
 * @author majianzheng
 */
public class TreeElement extends Element {
    private TreeElement parent = null;
    private List<TreeElement> children = new ArrayList<>();
    public TreeElement() {
        super();
    }
    public TreeElement(String node) {
        super(node);
    }

    public void addChild(TreeElement child) {
        child.parent = this;
        children.add(child);
    }

    @Override
    public String toHtml() {
        StringBuilder sb = new StringBuilder();
        //计算当前节点的层级

        TreeElement p = this;
        while (null != (p = p.parent)) {
            if (null == p.parent && StringUtils.isEmpty(p.text)) {
                //此时是root节点
                break;
            }
            sb.append("&nbsp;&nbsp;");
        }
        if (null != this.parent || !StringUtils.isEmpty(this.text)) {
            sb.append("+-").append(this.text).append("<br>");
        }

        children.forEach(child -> sb.append(child.toHtml()));
        return sb.toString();
    }
}
