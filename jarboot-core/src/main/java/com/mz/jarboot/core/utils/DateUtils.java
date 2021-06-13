package com.mz.jarboot.core.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
@SuppressWarnings("all")
public class DateUtils {

    private static final ThreadLocal<SimpleDateFormat> dataFormat = new ThreadLocal<SimpleDateFormat>() {

        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };

    public static String getCurrentDate() {
        return dataFormat.get().format(new Date());
    }

    public static String formatDate(Date date) {
        return dataFormat.get().format(date);
    }
}
