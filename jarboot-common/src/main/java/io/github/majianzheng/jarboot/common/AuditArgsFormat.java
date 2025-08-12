package io.github.majianzheng.jarboot.common;

/**
 * 审计日志参数格式化
 * @author majianzheng
 */
public interface AuditArgsFormat {
    /**
     * 格式化参数
     * @param args 参数数组
     * @return 格式化后的参数
     */
    String format(Object[] args);
}
