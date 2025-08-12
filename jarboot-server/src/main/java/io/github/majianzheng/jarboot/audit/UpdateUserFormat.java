package io.github.majianzheng.jarboot.audit;

import io.github.majianzheng.jarboot.common.AuditArgsFormat;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 用户更新日志格式化
 * @author majianzheng
 */
public class UpdateUserFormat implements AuditArgsFormat {
    @Override
    public String format(Object[] args) {
        if (null != args && args.length > 0) {
            List<String> argList = new ArrayList<>();
            for (int i = 0; i < args.length-1; i++) {
                Object arg = args[i];
                Class<?> cls = arg.getClass();
                if (cls.isPrimitive() || cls.isArray() || arg instanceof String || arg instanceof Collection || cls.getName().startsWith("io.github.majianzheng.jarboot.")) {
                    argList.add(JsonUtils.toJsonString(arg));
                }
            }
            String arg = String.join(",", argList);
            final int maxLength = 2000;
            if (arg.length() > maxLength) {
                arg = arg.substring(0, maxLength);
            }
            return arg;
        }
        return StringUtils.EMPTY;
    }
}
