package io.github.majianzheng.jarboot.common.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Json序列化、反序列化工具类
 * @author majianzheng
 */
public class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * 解析字符串为 {@link JsonNode}
     * @param content 字符串内容
     * @return JsonNode
     */
    public static JsonNode readAsJsonNode(String content) {
        try {
            return MAPPER.readValue(content, JsonNode.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析字节码为 {@link JsonNode}
     * @param content 字符串内容
     * @return JsonNode
     */
    public static JsonNode readAsJsonNode(byte[] content) {
        try {
            return MAPPER.readValue(content, JsonNode.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析输入流为 {@link JsonNode}
     * @param is 输入流
     * @return JsonNode
     */
    public static JsonNode readAsJsonNode(InputStream is) {
        try {
            return MAPPER.readValue(is, JsonNode.class);
        } catch (Exception e) {
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
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * json反序列化为对象
     * @param content 字节码
     * @param cls 类
     * @param <T> 类型
     * @return 对象
     */
    public static <T> T readValue(byte[] content, Class<T> cls) {
        try {
            return MAPPER.readValue(content, cls);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * json反序列化为对象
     * @param is 输入流
     * @param cls 类
     * @param <T> 类型
     * @return 对象
     */
    public static <T> T readValue(InputStream is, Class<T> cls) {
        try {
            return MAPPER.readValue(is, cls);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * json反序列化为对象列表
     * @param content 字符串
     * @param cls 类
     * @return 对象列表
     * @param <T> 类型
     */
    public static <T> List<T> readList(String content, Class<T> cls) {
        JsonNode node = readAsJsonNode(content);
        return toList(node, cls);
    }

    /**
     * json反序列化为对象列表
     * @param content 字符串
     * @param cls 类
     * @return 对象列表
     * @param <T> 类型
     */
    public static <T> List<T> readList(byte[] content, Class<T> cls) {
        JsonNode node = readAsJsonNode(content);
        return toList(node, cls);
    }

    /**
     * json反序列化为对象列表
     * @param is 输入流
     * @param cls 类
     * @return 对象列表
     * @param <T> 类型
     */
    public static <T> List<T> readList(InputStream is, Class<T> cls) {
        JsonNode node = readAsJsonNode(is);
        return toList(node, cls);
    }

    /**
     * JsonNode转对象列表
     * @param node jsonNode
     * @param cls 类
     * @return 对象列表
     * @param <T> 类型
     */
    public static <T> List<T> toList(JsonNode node, Class<T> cls) {
        List<T> value = new ArrayList<>();
        if (null == node) {
            return value;
        }
        if (node.isArray()) {
            final int size = node.size();
            for (int i = 0; i < size; ++i) {
                JsonNode obj = node.get(i);
                value.add(treeToValue(obj, cls));
            }
        }
        return value;
    }

    /**
     * json反序列化为对象
     * @param content 字符串
     * @param cls 类
     * @param <T> 类型
     * @return 对象
     */
    public static <T> T treeToValue(JsonNode content, Class<T> cls) {
        try {
            return MAPPER.treeToValue(content, cls);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 对象序列化为字符串
     * @param obj 对象
     * @return json字符串
     */
    public static String toJsonString(Object obj) {
        if (null == obj) {
            return StringUtils.NULL_STR;
        }
        if (obj instanceof String) {
            return (String)obj;
        }
        if (obj.getClass().isPrimitive()) {
            return obj.toString();
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return StringUtils.EMPTY;
        }
    }

    /**
     * 对象序列化为格式化的字符串
     * @param obj 对象
     * @return json字符串
     */
    public static String toPrettyJsonString(Object obj) {
        if (null == obj) {
            return StringUtils.NULL_STR;
        }
        if (obj instanceof String) {
            return (String)obj;
        }
        if (obj.getClass().isPrimitive()) {
            return obj.toString();
        }
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return StringUtils.EMPTY;
        }
    }

    /**
     * 对象序列化为字节码
     * @param obj 对象
     * @return json字节码
     */
    public static byte[] toJsonBytes(Object obj) {
        try {
            return MAPPER.writeValueAsBytes(obj);
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private JsonUtils() {}
}
