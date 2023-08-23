package io.github.majianzheng.jarboot.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * @author majianzheng
 */
@Table(name = Privilege.TABLE_NAME, uniqueConstraints = {@UniqueConstraint(columnNames = {"role", "authCode"})})
@Entity
public class Privilege extends AbstractBaseEntity {
    public static final String TABLE_NAME = "jarboot_privilege";
    private String role;
    private String authCode;
    private Boolean permission;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public Boolean getPermission() {
        return permission;
    }

    public void setPermission(Boolean permission) {
        this.permission = permission;
    }
}
