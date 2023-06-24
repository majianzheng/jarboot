package com.mz.jarboot.core.cmd.view;


import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.common.utils.StringUtils;

/**
 * @author majianzheng
 */
public class RowAffectView implements ResultView<com.mz.jarboot.core.cmd.model.RowAffectModel> {
    @Override
    public String render(CommandSession session, com.mz.jarboot.core.cmd.model.RowAffectModel result) {
        return (result.affect() + StringUtils.LF);
    }
}
