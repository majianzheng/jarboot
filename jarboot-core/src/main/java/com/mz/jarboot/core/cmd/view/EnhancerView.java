package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.core.cmd.model.EnhancerModel;
import com.mz.jarboot.common.utils.StringUtils;

/**
 * Term view for EnhancerModel
 * @author majianzheng
 */
public class EnhancerView implements ResultView<EnhancerModel> {
    @Override
    public String render(CommandSession session, EnhancerModel result) {
        // ignore enhance result status, judge by the following output
        if (result.getEffect() != null) {
            return ViewRenderUtil.renderEnhancerAffect(result.getEffect());
        }
        return StringUtils.EMPTY;
    }
}
