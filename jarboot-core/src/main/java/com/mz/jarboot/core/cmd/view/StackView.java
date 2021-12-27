package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.core.cmd.model.StackModel;
import com.mz.jarboot.core.utils.DateUtils;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.core.utils.ThreadUtil;

/**
 * Term view for StackModel
 * @author majianzheng
 */
public class StackView implements ResultView<StackModel> {

    @Override
    public String render(StackModel result) {
        StringBuilder sb = new StringBuilder();
        sb
                .append("ts=")
                .append(DateUtils.formatDate(result.getTs()))
                .append(";")
                .append(ThreadUtil.getThreadTitle(result)).append(StringUtils.LF);

        StackTraceElement[] stackTraceElements = result.getStackTrace();
        StackTraceElement locationStackTraceElement = stackTraceElements[0];
        String locationString = String.format("    @%s.%s()", locationStackTraceElement.getClassName(),
                locationStackTraceElement.getMethodName());
        sb.append(locationString).append(StringUtils.LF);

        int skip = 1;
        for (int index = skip; index < stackTraceElements.length; index++) {
            StackTraceElement ste = stackTraceElements[index];
            sb.append("        at ")
                    .append(ste.getClassName())
                    .append(".")
                    .append(ste.getMethodName())
                    .append("(")
                    .append(ste.getFileName())
                    .append(":")
                    .append(ste.getLineNumber())
                    .append(")\n\n");
        }
        return sb.toString();
    }

}
