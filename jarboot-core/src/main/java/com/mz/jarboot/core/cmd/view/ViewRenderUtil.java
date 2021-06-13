package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.core.basic.SingletonCoreFactory;
import com.mz.jarboot.core.cmd.model.EnhancerAffectVO;
import com.mz.jarboot.core.cmd.model.ThreadVO;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import static java.lang.String.format;


/**
 * view render util
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class ViewRenderUtil {

    /** Thread State Colors */
    public static final EnumMap<Thread.State, String> colorMapping = new EnumMap<Thread.State, String>(Thread.State.class);
    static {
        colorMapping.put(Thread.State.NEW, "cyan");
        colorMapping.put(Thread.State.RUNNABLE, "green");
        colorMapping.put(Thread.State.BLOCKED, "red");
        colorMapping.put(Thread.State.WAITING, "yellow");
        colorMapping.put(Thread.State.TIMED_WAITING, "magenta");
        colorMapping.put(Thread.State.TERMINATED, "blue");
    }

    /**
     * Render key-value table
     * @param map
     * @param width
     * @return
     */
//    public static String renderKeyValueTable(Map<String, String> map, int width) {
//        TableElement table = new TableElement(1, 4).leftCellPadding(1).rightCellPadding(1);
//        table.row(true, label("KEY").style(Decoration.bold.bold()), label("VALUE").style(Decoration.bold.bold()));
//
//        for (Map.Entry<String, String> entry : map.entrySet()) {
//            table.row(entry.getKey(), entry.getValue());
//        }
//
//        return RenderUtil.render(table, width);
//    }

    /**
     * Render change result vo
     * @param result
     * @return
     */
//    public static TableElement renderChangeResult(ChangeResultVO result) {
//        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
//        table.row(true, label("NAME").style(Decoration.bold.bold()),
//                label("BEFORE-VALUE").style(Decoration.bold.bold()),
//                label("AFTER-VALUE").style(Decoration.bold.bold()));
//        table.row(result.getName(), StringUtils.objectToString(result.getBeforeValue()),
//                StringUtils.objectToString(result.getAfterValue()));
//        return table;
//    }

    /**
     * Render EnhancerAffectVO
     * @param affectVO
     * @return
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
            infoSB.append("\nEnhance error! exception: " + affectVO.getThrowable());
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
            String color = colorMapping.get(thread.getState());
            String time = formatTimeMills(thread.getTime());
            String deltaTime = formatTimeMillsToSeconds(thread.getDeltaTime());
            double cpu = thread.getCpu();

            String daemonLabel = Boolean.toString(thread.isDaemon());
            if (!thread.isDaemon()) {
                daemonLabel = format("<span style=\"color:magenta\">%s</span>", daemonLabel);
            }
            String stateElement = "-";
            if (thread.getState() != null) {
                stateElement = format("<span style=\"color:%s\">%s</span>", color, thread.getState());
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
        TemplateEngine engine = SingletonCoreFactory.getInstance().createTemplateEngine();
        Context context = new Context();
        context.setVariable("headers", headers);
        context.setVariable("rows", rows);
        context.setVariable("title", title);
        return engine.process("template/TableView.html", context);
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
}
