package com.mz.jarboot.core.cmd.view.element;

import com.mz.jarboot.text.ui.TreeElement;
import com.mz.jarboot.text.util.RenderUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author majianzheng
 */
public class TreeElementTest {
    @Test
    public void test() {
        TreeElement tree = new TreeElement("root");
        TreeElement child1 = new TreeElement("child1");
        TreeElement child2 = new TreeElement("child2");
        tree.addChild(child1);
        tree.addChild(child2);
        //简单2层树
        assertEquals("+-root<br>&nbsp;&nbsp;+-child1<br>&nbsp;&nbsp;+-child2<br>", RenderUtil.render(tree));

        //3层树展示
        TreeElement child3 = new TreeElement("child23");
        child2.addChild(child3);
        assertEquals("+-root<br>&nbsp;&nbsp;+-child1<br>&nbsp;&nbsp;+-child2<br>&nbsp;&nbsp;&nbsp;&nbsp;+-child23<br>", RenderUtil.render(tree));

        //root节点为空字符
        assertEquals("+-child1<br>+-child2<br>&nbsp;&nbsp;+-child23<br>", RenderUtil.render(tree));
    }
}
