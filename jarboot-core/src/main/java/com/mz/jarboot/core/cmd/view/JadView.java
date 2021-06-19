package com.mz.jarboot.core.cmd.view;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * @author majianzheng
 */
@SuppressWarnings("all")
public class JadView implements ResultView<com.mz.jarboot.core.cmd.model.JadModel> {

    @Override
    public String render(com.mz.jarboot.core.cmd.model.JadModel result) {
        //mappings = result.getMappings();
        //todo fastjson的bug，map的key为数值时转化错误，未加引号，导致前端解析失败，暂不传输mappings，后续解决
        result.setMappings(null);
        return JSON.toJSONString(result, SerializerFeature.DisableCircularReferenceDetect);
    }

    @Override
    public boolean isJson() {
        return true;
    }
}
