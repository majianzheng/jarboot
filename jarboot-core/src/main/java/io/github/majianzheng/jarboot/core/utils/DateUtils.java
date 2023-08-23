package io.github.majianzheng.jarboot.core.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
@SuppressWarnings({"java:S1118", "java:S5164", "java:S4065"})
public class DateUtils {

    private static final ThreadLocal<SimpleDateFormat> DATA_FORMAT = new ThreadLocal<SimpleDateFormat>() {

        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };

    public static String getCurrentDate() {
        return DATA_FORMAT.get().format(new Date());
    }

    public static String formatDate(Date date) {
        return DATA_FORMAT.get().format(date);
    }
}
