package io.github.majianzheng.jarboot.dao;

import io.github.majianzheng.jarboot.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 审计日志
 * @author majianzheng
 */
@Repository
public interface AuditLogDao extends JpaRepository<AuditLog, Long> {

    /**
     * 新增日志
     * @param username 用户名
     * @param operation 操作
     * @param arg 参数
     * @param method 方法名
     * @param remoteIp 远程IP
     */
    default void newAuditLog(String username, String operation, String arg, String method, String remoteIp) {
        AuditLog log = new AuditLog();
        log.setUsername(username);
        log.setOperation(operation);
        log.setArgument(arg);
        log.setMethod(method);
        log.setRemoteIp(remoteIp);
        save(log);
    }
}
