package com.mz.jarboot.core.cmd.view;


/**
 * @author majianzheng
 */
public class RowAffectView implements ResultView<com.mz.jarboot.core.cmd.model.RowAffectModel> {
    @Override
    public String render(com.mz.jarboot.core.cmd.model.RowAffectModel result) {
        return (result.affect() + "\n");
    }
}
