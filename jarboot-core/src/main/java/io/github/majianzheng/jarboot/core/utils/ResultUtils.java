package io.github.majianzheng.jarboot.core.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 命令结果处理工具类
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
@SuppressWarnings("squid:S1854")
public class ResultUtils {

    /**
     * 分页处理class列表，转换为className列表
     * @param classes class
     * @param pageSize page size
     * @param handler handler
     */
    public static void processClassNames(Collection<Class<?>> classes, int pageSize, PaginationHandler<List<String>> handler) {
        List<String> classNames = new ArrayList<>(pageSize);
        int segment = 0;
        for (Class<?> aClass : classes) {
            classNames.add(aClass.getName());
            //slice segment
            if(classNames.size() >= pageSize) {
                handler.handle(classNames, segment++);
                classNames = new ArrayList<>(pageSize);
            }
        }
        //last segment
        if (!classNames.isEmpty()) {
            handler.handle(classNames, segment++);
        }
    }

    /**
     * 分页数据处理回调接口
     * @param <T>
     */
    public interface PaginationHandler<T> {

        /**
         * 处理分页数据
         * @param list
         * @param segment
         * @return  true 继续处理剩余数据， false 终止处理
         */
        boolean handle(T list, int segment);
    }
}
