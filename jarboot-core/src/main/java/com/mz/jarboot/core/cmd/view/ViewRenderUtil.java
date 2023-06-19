package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.common.AnsiLog;
import com.mz.jarboot.core.cmd.model.ChangeResultVO;
import com.mz.jarboot.core.cmd.model.EnhancerAffectVO;
import com.mz.jarboot.core.cmd.model.ThreadVO;
import com.mz.jarboot.core.cmd.view.element.TableElement;
import com.mz.jarboot.core.utils.HtmlNodeUtils;
import com.mz.jarboot.common.utils.StringUtils;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
     * @return table
     */
    public static String renderKeyValueTable(Map<String, String> map) {
        TableElement table = new TableElement();
        table.row(true, "KEY", "VALUE");

        for (Map.Entry<String, String> entry : map.entrySet()) {
            table.row(entry.getKey(), entry.getValue());
        }

        return table.toHtml();
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

    public static String drawThreadInfo(List<ThreadVO> threads, int height) {
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

        int count = 0;
        List<List<String>> rows = new ArrayList<>();
        for (ThreadVO thread : threads) {
            List<String> row = new ArrayList<>();
            String time = formatTimeMills(thread.getTime());
            String deltaTime = formatTimeMillsToSeconds(thread.getDeltaTime());
            double cpu = thread.getCpu();

            String daemonLabel = Boolean.toString(thread.isDaemon());
            if (!thread.isDaemon()) {
                daemonLabel = AnsiLog.magenta(daemonLabel);
            }
            String stateElement = "-";
            if (thread.getState() != null) {
                switch (thread.getState()) {
                    case NEW:
                        stateElement = AnsiLog.cyan(thread.getState().toString());
                        break;
                    case RUNNABLE:
                        stateElement = AnsiLog.green(thread.getState().toString());
                        break;
                    case BLOCKED:
                        stateElement = AnsiLog.red(thread.getState().toString());
                        break;
                    case WAITING:
                        stateElement = AnsiLog.yellow(thread.getState().toString());
                        break;
                    case TIMED_WAITING:
                        stateElement = AnsiLog.magenta(thread.getState().toString());
                        break;
                    case TERMINATED:
                        stateElement = AnsiLog.blue(thread.getState().toString());
                        break;
                    default:
                        break;
                }
            }
            row.add(String.valueOf(thread.getId()));
            row.add(thread.getName());
            row.add(thread.getGroup() != null ? thread.getGroup() : "-");
            row.add(String.valueOf(thread.getPriority()));
            row.add(stateElement);
            row.add(String.valueOf(cpu));
            row.add(deltaTime);
            row.add(time);
            row.add(String.valueOf(thread.isInterrupted()));
            row.add(daemonLabel);
            rows.add(row);
            if (++count >= height) {
                break;
            }
        }
        return renderTable(headers, rows, "THREAD");
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

    public static String renderTable(List<String> headers, List<List<String>> rows, String title) {
        return renderTable(headers, rows, title, 1);
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

    public static String renderTable(List<String> headers, List<List<String>> rows, String title, int border) {
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
        List<Integer> colWidth = new ArrayList<>();
        for (int i = 0; i < headers.size(); ++i) {
            colWidth.add(i, headers.get(i).length());
        }
        for (List<String> row : rows) {
            for (int j = 0; j < row.size(); ++j) {
                String c = row.get(j);
                int len = c.length();
                if (colWidth.size() < (j + 1)) {
                    colWidth.add(len);
                } else {
                    len = Math.max(len, colWidth.get(j));
                    colWidth.set(j, len);
                }
            }
        }
        int width = 2;
        for (Integer len : colWidth) {
            width += len + 1;
        }
        StringBuilder sb = new StringBuilder();

        if (StringUtils.isEmpty(title)) {
            sb.append("\n┌");
            for (int i = 0; i < colWidth.size(); ++i) {
                int w = colWidth.get(i);
                for (int j = 0; j < w; ++j) {
                    sb.append("─");
                }
                if ((i + 1) != colWidth.size()) {
                    sb.append('┬');
                }
            }
            sb.append("┐\n");
        } else {
            sb.append("\n┌");
            for (int i = 1; i < width - 2; ++i) {
                sb.append("─");
            }
            sb.append("┐\n");
            sb.append("│");
            whitePrint(sb, width - colWidth.size(), title).append("│\n");
            if (headers.isEmpty() && rows.isEmpty()) {
                sb.append('└');
                for (int i = 1; i < width - 2; ++i) {
                    sb.append("─");
                }
                sb.append("┘\n");
            } else {
                sb.append('├');
                for (int i = 0; i < colWidth.size(); ++i) {
                    int w = colWidth.get(i);
                    for (int j = 0; j < w; ++j) {
                        sb.append("─");
                    }
                    if ((i + 1) != colWidth.size()) {
                        sb.append('┬');
                    }
                }
                sb.append("┤\n");
            }
        }
        if (!headers.isEmpty()) {
            sb.append('│');
            for (int i = 0; i < colWidth.size(); ++i) {
                String c = headers.get(i);
                int w = colWidth.get(i);
                whitePrint(sb, w, c);
                sb.append('│');
            }
            sb.append("\n");
            if (rows.isEmpty()) {
                sb.append('└');
                for (int i = 1; i < width - 1; ++i) {
                    sb.append("─");
                }
                sb.append("┘\n");
            }
        }
        if (rows.isEmpty()) {
            return sb.toString();
        }
        for (int n = 0; n < rows.size(); ++n) {
            List<String> row = rows.get(n);
            if (!headers.isEmpty()) {
                sb.append('├');
                for (int i = 0; i < colWidth.size(); ++i) {
                    int w = colWidth.get(i);
                    for (int j = 0; j < w; ++j) {
                        sb.append("─");
                    }
                    if ((i + 1) != colWidth.size()) {
                        sb.append('┼');
                    }
                }
                sb.append("┤\n");
            }
            sb.append("│");
            for (int i = 0; i < colWidth.size(); ++i) {
                String c = row.get(i);
                int w = colWidth.get(i);
                whitePrint(sb, w, c);
                sb.append('│');
            }
            sb.append("\n");
        }
        sb.append('└');
        for (int i = 0; i < colWidth.size(); ++i) {
            int w = colWidth.get(i);
            for (int j = 0; j < w; ++j) {
                sb.append("─");
            }
            if ((i + 1) != colWidth.size()) {
                sb.append("┴");
            }
        }
        sb.append("┘\n");

        return sb.toString();
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
