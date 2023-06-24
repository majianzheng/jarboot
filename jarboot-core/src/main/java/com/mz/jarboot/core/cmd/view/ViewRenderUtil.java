package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.core.cmd.model.ChangeResultVO;
import com.mz.jarboot.core.cmd.model.EnhancerAffectVO;
import com.mz.jarboot.core.cmd.model.ThreadVO;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.text.Color;
import com.mz.jarboot.text.Style;
import com.mz.jarboot.text.ui.*;
import com.mz.jarboot.text.util.RenderUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mz.jarboot.text.ui.Element.label;
import static com.mz.jarboot.text.ui.Element.row;
import static java.lang.String.format;


/**
 * view render util
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
@SuppressWarnings({"squid:S1319", "squid:S2386", "PMD.UndefineMagicConstantRule"})
public class ViewRenderUtil {
    /**
     * Render key-value table
     * @param map map
     * @param width width
     * @return table
     */
    public static String renderKeyValueTable(Map<String, String> map, int width) {
        TableElement table = new TableElement(1, 1).border(BorderStyle.DASHED).separator(BorderStyle.DASHED).rightCellPadding(1).rightCellPadding(1);
        table.row(true, "KEY", "VALUE");

        for (Map.Entry<String, String> entry : map.entrySet()) {
            table.row(entry.getKey(), entry.getValue());
        }

        return RenderUtil.render(table, width);
    }

    /**
     * Render change result vo
     * @param result result
     * @return table element
     */
    public static TableElement renderChangeResult(ChangeResultVO result) {
        TableElement table = new TableElement();
        table.row(true, "NAME", "BEFORE-VALUE", "AFTER-VALUE");
        table.row(result.getName(), StringUtils.objectToString(result.getBeforeValue()),
                StringUtils.objectToString(result.getAfterValue()));
        return table;
    }

    /**
     * Render EnhancerAffectVO
     * @param affectVO affect
     * @return string
     */
    public static String renderEnhancerAffect(EnhancerAffectVO affectVO) {
        final StringBuilder infoSB = new StringBuilder();
        List<String> classDumpFiles = affectVO.getClassDumpFiles();
        if (classDumpFiles != null) {
            for (String classDumpFile : classDumpFiles) {
                infoSB.append("[dump: ").append(classDumpFile).append("]\n");
            }
        }

        List<String> methods = affectVO.getMethods();
        if (methods != null) {
            for (String method : methods) {
                infoSB.append("[Affect method: ").append(method).append("]\n");
            }
        }

        infoSB.append(format("Affect(class count: %d , method count: %d) cost in %s ms, listenerId: %d",
                affectVO.getClassCount(),
                affectVO.getMethodCount(),
                affectVO.getCost(),
                affectVO.getListenerId()));

        if (affectVO.getThrowable() != null) {
            infoSB
                    .append("\nEnhance error! exception: ")
                    .append(affectVO.getThrowable());
        }
        infoSB.append("\n");

        return infoSB.toString();
    }

    public static String drawThreadInfo(List<ThreadVO> threads, int width, int height) {
        // Header
        List<String> headers = new ArrayList<>();
        headers.add("ID");
        headers.add("NAME");
        headers.add("GROUP");
        headers.add("PRIORITY");
        headers.add("STATE");
        headers.add("%CPU");
        headers.add("DELTA_TIME");
        headers.add("TIME");
        headers.add("INTERRUPTED");
        headers.add("DAEMON");
        TableElement tableElement = createTableElement(headers);
        int count = 0;
        for (ThreadVO thread : threads) {
            List<LabelElement> row = new ArrayList<>();
            String time = formatTimeMills(thread.getTime());
            String deltaTime = formatTimeMillsToSeconds(thread.getDeltaTime());
            double cpu = thread.getCpu();

            LabelElement daemonLabel = label(Boolean.toString(thread.isDaemon()));
            if (!thread.isDaemon()) {
                daemonLabel = daemonLabel.style(Style.style(Color.magenta));
            }
            LabelElement stateElement = label("-");
            if (thread.getState() != null) {
                switch (thread.getState()) {
                    case NEW:
                        stateElement = label(thread.getState().toString()).style(Style.style(Color.cyan));
                        break;
                    case RUNNABLE:
                        stateElement = label(thread.getState().toString()).style(Style.style(Color.green));
                        break;
                    case BLOCKED:
                        stateElement = label(thread.getState().toString()).style(Style.style(Color.red));
                        break;
                    case WAITING:
                        stateElement = label(thread.getState().toString()).style(Style.style(Color.yellow));
                        break;
                    case TIMED_WAITING:
                        stateElement = label(thread.getState().toString()).style(Style.style(Color.magenta));
                        break;
                    case TERMINATED:
                        stateElement = label(thread.getState().toString()).style(Style.style(Color.blue));
                        break;
                    default:
                        break;
                }
            }
            row.add(label(String.valueOf(thread.getId())));
            row.add(label(thread.getName()));
            row.add(label(thread.getGroup() != null ? thread.getGroup() : "-"));
            row.add(label(String.valueOf(thread.getPriority())));
            row.add(stateElement);
            LabelElement cpuLabel = label(String.valueOf(cpu));
            if (cpu < 1) {
                cpuLabel.style(Style.style(Color.green));
            } else if (cpu < 5) {
                cpuLabel.style(Style.style(Color.yellow));
            } else {
                cpuLabel.style(Style.style(Color.red));
            }
            row.add(cpuLabel);
            row.add(label(deltaTime));
            row.add(label(time));
            row.add(label(String.valueOf(thread.isInterrupted())));
            row.add(daemonLabel);
            tableElement.row(row.toArray(new LabelElement[0]));
            if (++count >= height) {
                break;
            }
        }
        return RenderUtil.render(tableElement, width);
    }

    private static String formatTimeMills(long timeMills) {
        long seconds = timeMills / 1000;
        long mills = timeMills % 1000;
        long min = seconds / 60;
        seconds = seconds % 60;

        String str;
        if (mills >= 100) {
            str = min + ":" + seconds + "." + mills;
        } else if (mills >= 10) {
            str = min + ":" + seconds + ".0" + mills;
        } else {
            str = min + ":" + seconds + ".00" + mills;
        }
        return str;
    }

    public static String renderTable(List<String> headers, List<List<String>> rows, int with) {
        return renderTable(headers, rows, with, 1);
    }

    public static String renderTableHtml(List<String> headers, List<List<String>> rows, String title, int border) {
        if (null == headers) {
            headers = new ArrayList<>();
        }
        if (null == rows) {
            rows = new ArrayList<>();
        }
        if (null == title) {
            title = StringUtils.EMPTY;
        }
        if (border < 0) {
            border = 0;
        }
        StringBuilder tableBuilder = new StringBuilder();
        tableBuilder
                .append("<table border=\"")
                .append(border).append("\">");
        if (!title.isEmpty()) {
            tableBuilder
                    .append("<caption style=\"caption-side: top; font-size: 20px; color: snow\">")
                    .append(title).append("</caption>");
        }
        tableBuilder.append("<tbody>");
        //是否有列头
        if (!headers.isEmpty()) {
            tableBuilder.append("<tr>");
            headers.forEach(header ->
                    tableBuilder
                            .append("<th>")
                            .append(null == header ? StringUtils.EMPTY : header)
                            .append("</th>"));
            tableBuilder.append("</tr>");
        }
        if (!rows.isEmpty()) {
            rows.forEach(row -> {
                tableBuilder.append("<tr>");
                row.forEach(cell ->
                        tableBuilder
                                .append("<td>")
                                .append(null == cell ? StringUtils.EMPTY : cell)
                                .append("</td>"));
                tableBuilder.append("</tr>");
            });
        }
        tableBuilder.append("</tbody>");
        tableBuilder.append("</table>");
        return tableBuilder.toString();
    }

    public static String renderTable(List<String> headers, List<List<String>> rows, int width, int border) {
        TableElement tableElement = createTableElement(headers);

        // 设置第一列输出字体蓝色，红色背景
        // 设置第二列字体加粗，加下划线
        for (List<String> row : rows) {
            RowElement tableRow = row();
            for (String cel : row) {
                tableRow.add(label(cel));
            }
            tableElement.add(tableRow);
        }

        // 默认输出宽度是80
        return RenderUtil.render(tableElement, width);
    }

    @NotNull
    private static TableElement createTableElement(List<String> headers) {
        // 设置两列的比例是1:1，如果不设置的话，列宽是自动按元素最长的处理。
        // 设置table的外部边框，默认是没有外边框
        // 还有内部的分隔线，默认内部没有分隔线
        TableElement tableElement = new TableElement().border(BorderStyle.DASHED).separator(BorderStyle.DASHED);

        // 设置单元格的左右边框间隔，默认是没有，看起来会有点挤，空间足够时，可以设置为1，看起来清爽
        tableElement.leftCellPadding(1).rightCellPadding(1);

        // 设置header
        if (null != headers) {
            tableElement.row(true, headers.stream().map(col -> label(col).style(Style.style().bold(true))).toArray(LabelElement[]::new));
        }

        // 设置cell里的元素超出了处理方式，Overflow.HIDDEN 表示隐藏
        // Overflow.WRAP表示会向外面排出去，即当输出宽度有限时，右边的列可能会显示不出，被挤掉了
        tableElement.overflow(Overflow.HIDDEN);
        return tableElement;
    }

    private static StringBuilder whitePrint(StringBuilder sb, int width, String text) {
        int margin = (width - text.length()) / 2;
        for (int i = 0; i < margin; ++i) {
            sb.append(' ');
        }
        sb.append(text);
        int right = margin < 0 ? 0 : (width - margin - text.length());
        for (int i = 0; i < right; ++i) {
            sb.append(' ');
        }
        return sb;
    }

    private static String formatTimeMillsToSeconds(long timeMills) {
        long seconds = timeMills / 1000;
        long mills = timeMills % 1000;

        String str;
        if (mills >= 100) {
            str = seconds + "." + mills;
        } else if (mills >= 10) {
            str = seconds + ".0" + mills;
        } else {
            str = seconds + ".00" + mills;
        }
        return str;
    }

    private ViewRenderUtil() {}
}
