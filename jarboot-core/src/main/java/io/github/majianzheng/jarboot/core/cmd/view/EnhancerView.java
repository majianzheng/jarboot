package io.github.majianzheng.jarboot.core.cmd.view;

import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.core.cmd.model.EnhancerModel;
import io.github.majianzheng.jarboot.common.utils.StringUtils;

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
