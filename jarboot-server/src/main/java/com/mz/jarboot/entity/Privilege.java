package com.mz.jarboot.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * @author majianzheng
 */
@Table(name = Privilege.TABLE_NAME, uniqueConstraints = {@UniqueConstraint(columnNames = {"role", "resource"})})
@Entity
public class Privilege extends AbstractBaseEntity {
    public static final String TABLE_NAME = "jarboot_privilege";
    private String role;
    private String resource;
    private Boolean permission;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Boolean getPermission() {
        return permission;
    }

    public void setPermission(Boolean permission) {
        this.permission = permission;
    }
}
