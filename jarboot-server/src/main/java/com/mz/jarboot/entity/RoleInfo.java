package com.mz.jarboot.entity;

import javax.persistence.UniqueConstraint;

/**
 * @author majianzheng
 */
@javax.persistence.Table(name = RoleInfo.TABLE_NAME, uniqueConstraints = {@UniqueConstraint(columnNames = {"role", "username"})})
@javax.persistence.Entity
public class RoleInfo extends AbstractBaseEntity {
    public static final String TABLE_NAME = "jarboot_role";
    private String role;
    private String username;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
