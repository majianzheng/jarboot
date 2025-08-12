package io.github.majianzheng.jarboot.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 审计日志
 * @author mazheng
 */
@Table(name = AuditLog.TABLE_NAME)
@Entity
public class AuditLog extends AbstractBaseEntity {
    public static final String TABLE_NAME = "audit_log";
    private String username;

    private String operation;

    private String method;

    private String argument;

    private String remoteIp;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Column(length = 4000)
    public String getArgument() {
        return argument;
    }

    public void setArgument(String argument) {
        this.argument = argument;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }
}
