package io.github.majianzheng.jarboot.core.cmd.view;

import io.github.majianzheng.jarboot.core.cmd.model.ChangeResultVO;
import io.github.majianzheng.jarboot.core.cmd.model.EnhancerAffectVO;
import io.github.majianzheng.jarboot.core.cmd.model.ThreadVO;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.text.Color;
import io.github.majianzheng.jarboot.text.Style;
import io.github.majianzheng.jarboot.text.ui.*;
import io.github.majianzheng.jarboot.text.util.RenderUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.majianzheng.jarboot.text.ui.Element.label;
import static java.lang.String.format;


/**
 * view render util
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
@SuppressWarnings({"squid:S1319", "java:S3776", "squid:S2386", "PMD.UndefineMagicConstantRule"})
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
        TableElement tableElement = RenderUtil.createTableElement(headers, BorderStyle.DASHED);
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
        return RenderUtil.renderTable(headers, rows, with, BorderStyle.DASHED);
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
