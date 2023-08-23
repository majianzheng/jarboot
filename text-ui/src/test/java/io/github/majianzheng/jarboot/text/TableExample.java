package io.github.majianzheng.jarboot.text;

import io.github.majianzheng.jarboot.text.ui.BorderStyle;
import io.github.majianzheng.jarboot.text.ui.Overflow;
import io.github.majianzheng.jarboot.text.ui.TableElement;
import io.github.majianzheng.jarboot.text.util.RenderUtil;
import io.github.majianzheng.jarboot.text.ui.Element;
import org.junit.Test;

public class TableExample {

    @Test
    public void test1() {
        // header定义
        String[] fields = { "name", "age" };

        // 设置两列的比例是1:1，如果不设置的话，列宽是自动按元素最长的处理。
        // 设置table的外部边框，默认是没有外边框
        // 还有内部的分隔线，默认内部没有分隔线
        TableElement tableElement = new TableElement(1, 1).border(BorderStyle.DASHED).separator(BorderStyle.DASHED);

        // 设置单元格的左右边框间隔，默认是没有，看起来会有点挤，空间足够时，可以设置为1，看起来清爽
        tableElement.leftCellPadding(1).rightCellPadding(1);

        // 设置header
        tableElement.row(true, fields);

        // 设置cell里的元素超出了处理方式，Overflow.HIDDEN 表示隐藏
        // Overflow.WRAP表示会向外面排出去，即当输出宽度有限时，右边的列可能会显示不出，被挤掉了
        tableElement.overflow(Overflow.HIDDEN);

        // 设置第一列输出字体蓝色，红色背景
        // 设置第二列字体加粗，加下划线
        for (int i = 0; i < 10; ++i) {
            tableElement.add(Element.row().add(Element.label("student" + i).style(Style.Composite.style(Color.blue).bg(Color.red)))
                    .add(Element.label("" + i).style(Decoration.bold.underline())));
        }

        // 默认输出宽度是80
        System.err.println(RenderUtil.render(tableElement));
    }
}
