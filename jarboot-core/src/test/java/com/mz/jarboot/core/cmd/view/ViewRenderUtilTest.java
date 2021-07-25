package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.common.MzException;
import com.mz.jarboot.core.cmd.model.ChangeResultVO;
import com.mz.jarboot.core.cmd.model.EnhancerAffectVO;
import com.mz.jarboot.core.cmd.view.element.TableElement;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author majianzheng
 */
public class ViewRenderUtilTest {
    @Test
    public void testRenderTable() {
        //正常测试，3行3列
        java.util.List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        String title = "title";
        headers.add("header1");
        headers.add("header2");
        headers.add("header3");
        List<String> row1 = new ArrayList<>();
        row1.add("col11");
        row1.add("col12");
        row1.add("col13");

        List<String> row2 = new ArrayList<>();
        row1.add("col21");
        row1.add("col22");
        row1.add("col23");

        List<String> row3 = new ArrayList<>();
        row1.add("col31");
        row1.add("col32");
        row1.add("col33");

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        String tableHtml = ViewRenderUtil.renderTable(headers, rows, title);
        String expect = "<table border=1><caption style=\"caption-side: top; font-size: 20px; color: snow\">title</caption><tbody><tr><th><span>header1</span></th><th><span>header2</span></th><th><span>header3</span></th></tr><tr><td><span>col11</span></td><td><span>col12</span></td><td><span>col13</span></td><td><span>col21</span></td><td><span>col22</span></td><td><span>col23</span></td><td><span>col31</span></td><td><span>col32</span></td><td><span>col33</span></td></tr><tr></tr><tr></tr></tbody></table>";
        assertEquals(expect, tableHtml);

        //标题为空，此时无caption
        tableHtml = ViewRenderUtil.renderTable(headers, rows, null);
        expect = "<table border=1><tbody><tr><th><span>header1</span></th><th><span>header2</span></th><th><span>header3</span></th></tr><tr><td><span>col11</span></td><td><span>col12</span></td><td><span>col13</span></td><td><span>col21</span></td><td><span>col22</span></td><td><span>col23</span></td><td><span>col31</span></td><td><span>col32</span></td><td><span>col33</span></td></tr><tr></tr><tr></tr></tbody></table>";
        assertEquals(expect, tableHtml);

        //header为null，此时无th
        tableHtml = ViewRenderUtil.renderTable(null, rows, null);
        expect = "<table border=1><tbody><tr><td><span>col11</span></td><td><span>col12</span></td><td><span>col13</span></td><td><span>col21</span></td><td><span>col22</span></td><td><span>col23</span></td><td><span>col31</span></td><td><span>col32</span></td><td><span>col33</span></td></tr><tr></tr><tr></tr></tbody></table>";
        assertEquals(expect, tableHtml);

        //行为空
        tableHtml = ViewRenderUtil.renderTable(headers, null, "demo");
        expect = "<table border=1><caption style=\"caption-side: top; font-size: 20px; color: snow\">demo</caption><tbody><tr><th><span>header1</span></th><th><span>header2</span></th><th><span>header3</span></th></tr></tbody></table>";
        assertEquals(expect, tableHtml);

        //border为2
        tableHtml = ViewRenderUtil.renderTable(headers, null, "demo", 2);
        expect = "<table border=2><caption style=\"caption-side: top; font-size: 20px; color: snow\">demo</caption><tbody><tr><th><span>header1</span></th><th><span>header2</span></th><th><span>header3</span></th></tr></tbody></table>";
        assertEquals(expect, tableHtml);

        //border为-1
        tableHtml = ViewRenderUtil.renderTable(headers, null, "demo", -1);
        expect = "<table border=0><caption style=\"caption-side: top; font-size: 20px; color: snow\">demo</caption><tbody><tr><th><span>header1</span></th><th><span>header2</span></th><th><span>header3</span></th></tr></tbody></table>";
        assertEquals(expect, tableHtml);
        //border为0
        tableHtml = ViewRenderUtil.renderTable(headers, null, "demo", 0);
        expect = "<table border=0><caption style=\"caption-side: top; font-size: 20px; color: snow\">demo</caption><tbody><tr><th><span>header1</span></th><th><span>header2</span></th><th><span>header3</span></th></tr></tbody></table>";
        assertEquals(expect, tableHtml);
    }

    @Test
    public void testRenderChangeResult() {
        ChangeResultVO result = new ChangeResultVO();
        TableElement table = ViewRenderUtil.renderChangeResult(result);
        String html = table.toHtml();
        assertEquals("<table border=1><tbody><tr><th><span>NAME</span></th><th><span>BEFORE-VALUE</span></th><th><span>AFTER-VALUE</span></th></tr><tr><td><span></span></td><td><span></span></td><td><span></span></td></tr></tbody></table>", html);
        result.setName("name1");
        result.setBeforeValue(123);
        result.setAfterValue(456);
        table = ViewRenderUtil.renderChangeResult(result);
        html = table.toHtml();
        System.out.println(html);
        assertEquals("<table border=1><tbody><tr><th><span>NAME</span></th><th><span>BEFORE-VALUE</span></th><th><span>AFTER-VALUE</span></th></tr><tr><td><span>name1</span></td><td><span>123</span></td><td><span>456</span></td></tr></tbody></table>", html);
    }

    @Test
    public void testRenderKeyValueTable() {
        Map<String, String> data = new HashMap<>();
        String tableHtml = ViewRenderUtil.renderKeyValueTable(data);
        //空表测试
        assertEquals("<table border=1><tbody><tr><th><span>KEY</span></th><th><span>VALUE</span></th></tr></tbody></table>", tableHtml);
        data.put("主键", "值");
        tableHtml = ViewRenderUtil.renderKeyValueTable(data);
        System.out.println(tableHtml);
        assertEquals("<table border=1><tbody><tr><th><span>KEY</span></th><th><span>VALUE</span></th></tr><tr><td><span>主键</span></td><td><span>值</span></td></tr></tbody></table>", tableHtml);
    }

    @Test
    public void testRenderEnhancerAffect() {
        EnhancerAffectVO affectVO = new EnhancerAffectVO(100, 2, 3, 4);
        String str = ViewRenderUtil.renderEnhancerAffect(affectVO);
        assertEquals("Affect(class count: 3 , method count: 2) cost in 100 ms, listenerId: 4\n", str);

        List<String> classDumpFiles = new ArrayList<>();
        classDumpFiles.add("file1");
        affectVO.setClassDumpFiles(classDumpFiles);
        List<String> methods = new ArrayList<>();
        methods.add("method1");
        affectVO.setMethods(methods);
        Throwable throwable = new MzException("test");
        affectVO.setThrowable(throwable);
        str = ViewRenderUtil.renderEnhancerAffect(affectVO);
        assertEquals("[dump: file1]\n" +
                "[Affect method: method1]\n" +
                "Affect(class count: 3 , method count: 2) cost in 100 ms, listenerId: 4\n" +
                "Enhance error! exception: com.mz.jarboot.common.MzException: test\n", str);
    }
}
