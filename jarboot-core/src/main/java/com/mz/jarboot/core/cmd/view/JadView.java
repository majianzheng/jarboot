package com.mz.jarboot.core.cmd.view;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * @author majianzheng
 */
public class JadView implements ResultView<com.mz.jarboot.core.cmd.model.JadModel> {

    @Override
    public String render(com.mz.jarboot.core.cmd.model.JadModel result) {
        return JSON.toJSONString(result, SerializerFeature.DisableCircularReferenceDetect);
    }

    @Override
    public boolean isJson() {
        return true;
    }
}
