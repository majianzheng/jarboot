package io.github.majianzheng.jarboot.core.cmd.view;

import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.core.utils.DateUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.core.cmd.model.WatchModel;


/**
 * Term view for WatchModel
 *
 * @author majianzheng
 */
public class WatchView implements ResultView<WatchModel> {

    @Override
    public String render(CommandSession session, WatchModel model) {
        Object value = model.getValue();
        String result = StringUtils.objectToString(
                isNeedExpand(model) ? new ObjectView(value, model.getExpand(), model.getSizeLimit()).draw() : value);

        String out = ("method=" + model.getClassName() + "." + model.getMethodName() + " location=" + model.getAccessPoint() + "\n");
        out += ("ts=" + DateUtils.formatDate(model.getTs()) + "; [cost=" + model.getCost() + "ms] result=" + result + "\n");
        return out;
    }

    private boolean isNeedExpand(WatchModel model) {
        Integer expand = model.getExpand();
        return null != expand && expand >= 0;
    }
}
