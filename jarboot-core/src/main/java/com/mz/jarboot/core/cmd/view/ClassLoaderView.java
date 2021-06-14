package com.mz.jarboot.core.cmd.view;

import com.alibaba.fastjson.JSON;

/**
 * @author majianzheng
 */
public class ClassLoaderView implements ResultView<com.mz.jarboot.core.cmd.model.ClassLoaderModel> {

    @Override
    public String render(com.mz.jarboot.core.cmd.model.ClassLoaderModel result) {
        return JSON.toJSONString(result);
    }

    @Override
    public boolean isJson() {
        return true;
    }
}
