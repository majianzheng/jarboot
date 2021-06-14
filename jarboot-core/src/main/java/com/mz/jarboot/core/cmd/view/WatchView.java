package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.core.utils.DateUtils;
import com.mz.jarboot.core.utils.StringUtils;


/**
 * Term view for WatchModel
 *
 * @author majianzheng
 */
public class WatchView implements ResultView<com.mz.jarboot.core.cmd.model.WatchModel> {

    @Override
    public String render(com.mz.jarboot.core.cmd.model.WatchModel model) {
        Object value = model.getValue();
        String result = StringUtils.objectToString(
                isNeedExpand(model) ? new ObjectView(value, model.getExpand(), model.getSizeLimit()).draw() : value);

        String out = ("method=" + model.getClassName() + "." + model.getMethodName() + " location=" + model.getAccessPoint() + "\n");
        out += ("ts=" + DateUtils.formatDate(model.getTs()) + "; [cost=" + model.getCost() + "ms] result=" + result + "\n");
        return out;
    }

    private boolean isNeedExpand(com.mz.jarboot.core.cmd.model.WatchModel model) {
        Integer expand = model.getExpand();
        return null != expand && expand >= 0;
    }
}
