package com.mz.jarboot.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Json序列化、反序列化工具类
 * @author jianzhengma
 */
public class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 解析字符串为 {@link JsonNode}
     * @param content 字符串内容
     * @return JsonNode
     */
    public static JsonNode readAsJsonNode(String content) {
        try {
            return MAPPER.readValue(content, JsonNode.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * json反序列化为对象
     * @param content 字符串
     * @param cls 类
     * @param <T> 类型
     * @return 对象
     */
    public static <T> T readValue(String content, Class<T> cls) {
        try {
            return MAPPER.readValue(content, cls);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 对象序列化为字符串
     * @param obj 对象
     * @return json字符串
     */
    public static String toJSONString(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private JsonUtils() {}
}
