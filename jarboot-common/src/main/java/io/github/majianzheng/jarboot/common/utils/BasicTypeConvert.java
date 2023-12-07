package io.github.majianzheng.jarboot.common.utils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 字符串转化为指定的类型数据
 * @author majianzheng
 */
@SuppressWarnings({"squid:S3776", "unchecked", "squid:S2259"})
public class BasicTypeConvert {
    private static final String ARRAY_SPLIT = ",";

    public static <T> T convert(String s, Class<T> cls) {
        if (StringUtils.isEmpty(s)) {
            return null;
        }
        Object val = convertBasic(s, cls);
        if (null != val) {
            return (T)val;
        }
        if (cls.isArray()) {
            //数组类型
            return (T)s.split(ARRAY_SPLIT);
        }
        //这里可以使用==直接比较引用，不必使用equals
        if (cls == Collection.class || cls == List.class) {
            //集合类型，构建ArrayList
            val = Arrays.stream(s.split(ARRAY_SPLIT)).collect(Collectors.toList());
        }
        return (T)val;
    }

    private static Object convertBasic(String val, Class<?> cls) {
        Object arg = null;
        //这里可以使用==直接比较引用，不必使用equals
        if (String.class == cls) {
            return val;
        }
        if (boolean.class == cls || Boolean.class == cls) {
            arg = Boolean.parseBoolean(val);
        } else if (char.class == cls || Character.class == cls) {
            arg = val.charAt(0);
        } else if (int.class == cls || Integer.class == cls) {
            arg = Integer.parseInt(val);
        } else if (byte.class == cls || Byte.class == cls) {
            arg = Byte.parseByte(val);
        } else if (short.class ==cls || Short.class == cls) {
            arg = Short.parseShort(val);
        } else if (long.class == cls || Long.class == cls) {
            arg = Long.parseLong(val);
        } else if (float.class == cls || Float.class == cls) {
            arg = Float.parseFloat(val);
        } else if (double.class == cls || Double.class == cls) {
            arg = Double.parseDouble(val);
        }
        return arg;
    }
    private BasicTypeConvert() {}
}
