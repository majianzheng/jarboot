package io.github.majianzheng.jarboot.core.cmd.view;

import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.core.cmd.model.StackModel;
import io.github.majianzheng.jarboot.core.utils.DateUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.core.utils.ThreadUtil;

/**
 * Term view for StackModel
 * @author majianzheng
 */
public class StackView implements ResultView<StackModel> {

    @Override
    public String render(CommandSession session, StackModel result) {
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
