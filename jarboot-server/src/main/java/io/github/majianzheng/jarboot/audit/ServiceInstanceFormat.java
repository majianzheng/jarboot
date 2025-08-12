package io.github.majianzheng.jarboot.audit;

import io.github.majianzheng.jarboot.api.pojo.ServiceInstance;
import io.github.majianzheng.jarboot.common.AuditArgsFormat;
import io.github.majianzheng.jarboot.common.utils.StringUtils;

import java.util.List;

/**
 * 服务实例审计日志参数格式化
 * @author majianzheng
 */
public class ServiceInstanceFormat implements AuditArgsFormat {
    @Override
    public String format(Object[] args) {
        Object obj = args[0];
        if (obj instanceof List) {
            List<ServiceInstance> instances = (List<ServiceInstance>) obj;
            return instances.stream().map(this::nameFormat).reduce((a, b) -> a + "," + b).orElse(StringUtils.EMPTY);
        }
        if (obj instanceof ServiceInstance) {
            return nameFormat((ServiceInstance) obj);
        }
        return StringUtils.EMPTY;
    }

    private String nameFormat(ServiceInstance instance) {
        if (StringUtils.isEmpty(instance.getHost())) {
            return instance.getName();
        }
        return instance.getName() + "@" + instance.getHost();
    }
}
