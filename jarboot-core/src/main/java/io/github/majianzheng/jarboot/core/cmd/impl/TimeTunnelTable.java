package io.github.majianzheng.jarboot.core.cmd.impl;

import io.github.majianzheng.jarboot.core.cmd.model.TimeFragmentVO;
import io.github.majianzheng.jarboot.core.cmd.view.ObjectView;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.text.ui.Element;
import io.github.majianzheng.jarboot.text.ui.TableElement;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author majianzheng
 */
@SuppressWarnings({"java:S1192", "java:S1118"})
public class TimeTunnelTable {
    /** 各列名称 */
    private static final String[] TABLE_COL_TITLE = new String[]{
            "INDEX",
            "TIMESTAMP",
            "COST(ms)",
            "IS-RET",
            "IS-EXP",
            "OBJECT",
            "CLASS",
            "METHOD"

    };

    static TableElement createTable() {
        return new TableElement();
    }

    public static TableElement createDefaultTable() {
        return new TableElement().rightCellPadding(1).leftCellPadding(1);
    }

    static TableElement fillTableHeader(TableElement table) {
        table.row(true, TABLE_COL_TITLE);
        return table;
    }

    public static Element drawTimeTunnelTable(List<TimeFragmentVO> timeFragmentList, boolean withHeader){
        // 绘制TimeTunnel表格
        TableElement table = createTable();
        if (withHeader) {
            fillTableHeader(table);
        }
        for (TimeFragmentVO tf : timeFragmentList) {
            fillTableRow(table, tf);
        }
        return table;
    }

    static TableElement fillTableRow(TableElement table, TimeFragmentVO tf) {
        // 填充表格行
        return table.row(
                "" + tf.getIndex(),
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(tf.getTimestamp()),
                "" + tf.getCost(),
                "" + tf.isReturn(),
                "" + tf.isThrow(),
                tf.getObject(),
                StringUtils.substringAfterLast("." + tf.getClassName(), "."),
                tf.getMethodName()
        );
    }

    public static void drawTimeTunnel(TableElement table, TimeFragmentVO tf) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        table
                .row("INDEX", "" + tf.getIndex())
                .row("GMT-CREATE", sdf.format(tf.getTimestamp()))
                .row("COST(ms)", "" + tf.getCost())
                .row("OBJECT", tf.getObject())
                .row("CLASS", tf.getClassName())
                .row("METHOD", tf.getMethodName())
                .row("IS-RETURN", "" + tf.isReturn())
                .row("IS-EXCEPTION", "" + tf.isThrow());
    }

    public static void drawThrowException(TableElement table, TimeFragmentVO tf, boolean isNeedExpand, Integer expandLevel) {
        if (tf.isThrow()) {
            //noinspection ThrowableResultOfMethodCallIgnored
            Throwable throwable = tf.getThrowExp();
            if (isNeedExpand) {
                table.row("THROW-EXCEPTION", new ObjectView(throwable, expandLevel).draw());
            } else {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                try {
                    throwable.printStackTrace(printWriter);
                    table.row("THROW-EXCEPTION", stringWriter.toString());
                } finally {
                    printWriter.close();
                }
            }
        }
    }

    public static void drawReturnObj(TableElement table, TimeFragmentVO tf, boolean isNeedExpand, Integer expandLevel, Integer sizeLimit) {
        if (tf.isReturn()) {
            if (isNeedExpand) {
                table.row("RETURN-OBJ", new ObjectView(tf.getReturnObj(), expandLevel, sizeLimit).draw());
            } else {
                table.row("RETURN-OBJ", "" + StringUtils.objectToString(tf.getReturnObj()));
            }
        }
    }

    public static void drawParameters(TableElement table, Object[] params, boolean isNeedExpand, Integer expandLevel) {
        if (params != null) {
            int paramIndex = 0;
            for (Object param : params) {
                if (isNeedExpand) {
                    table.row("PARAMETERS[" + paramIndex++ + "]", new ObjectView(param, expandLevel).draw());
                } else {
                    table.row("PARAMETERS[" + paramIndex++ + "]", "" + StringUtils.objectToString(param));
                }
            }
        }
    }

    public static void drawWatchTableHeader(TableElement table) {
        table.row(true, "INDEX", "SEARCH-RESULT");
    }

    public static void drawWatchResults(TableElement table, Map<Integer, Object> watchResults, boolean isNeedExpand,
                                        Integer expandLevel, Integer sizeLimit) {
        for (Map.Entry<Integer, Object> entry : watchResults.entrySet()) {
            Object value = entry.getValue();
            table.row("" + entry.getKey(), "" +
                    (isNeedExpand ? new ObjectView(value, expandLevel, sizeLimit).draw() : StringUtils.objectToString(value)));
        }
    }

    public static TableElement drawPlayHeader(String className, String methodName, String objectAddress, int index,
                                       TableElement table) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return table.row("RE-INDEX", "" + index)
                .row("GMT-REPLAY", sdf.format(new Date()))
                .row("OBJECT", objectAddress)
                .row("CLASS", className)
                .row("METHOD", methodName);
    }

    public static void drawPlayResult(TableElement table, Object returnObj, boolean isNeedExpand, int expandLevel,
                               int sizeLimit, double cost) {
        // 执行成功:输出成功状态
        table.row("IS-RETURN", "" + true);
        table.row("IS-EXCEPTION", "" + false);
        table.row("COST(ms)", "" + cost);

        // 执行成功:输出成功结果
        if (isNeedExpand) {
            table.row("RETURN-OBJ", new ObjectView(returnObj, expandLevel, sizeLimit).draw());
        } else {
            table.row("RETURN-OBJ", "" + StringUtils.objectToString(returnObj));
        }
    }

    public static void drawPlayException(TableElement table, Throwable t, boolean isNeedExpand, int expandLevel) {
        // 执行失败:输出失败状态
        table.row("IS-RETURN", "" + false);
        table.row("IS-EXCEPTION", "" + true);

        // 执行失败:输出失败异常信息
        Throwable cause;
        if (t instanceof InvocationTargetException) {
            cause = t.getCause();
        } else {
            cause = t;
        }

        if (isNeedExpand) {
            table.row("THROW-EXCEPTION", new ObjectView(cause, expandLevel).draw());
        } else {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            try {
                cause.printStackTrace(printWriter);
                table.row("THROW-EXCEPTION", stringWriter.toString());
            } finally {
                printWriter.close();
            }
        }
    }
}
