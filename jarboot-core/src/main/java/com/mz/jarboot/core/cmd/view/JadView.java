package com.mz.jarboot.core.cmd.view;


import com.mz.jarboot.common.JsonUtils;

/**
 * @author majianzheng
 */
public class JadView implements ResultView<com.mz.jarboot.core.cmd.model.JadModel> {

    @Override
    public String render(com.mz.jarboot.core.cmd.model.JadModel result) {
        return JsonUtils.toJSONString(result);
    }

    @Override
    public boolean isJson() {
        return true;
    }
}
