package io.github.majianzheng.jarboot.entity;

import javax.persistence.UniqueConstraint;

/**
 * @author majianzheng
 */
@javax.persistence.Table(name = RoleInfo.TABLE_NAME, uniqueConstraints = {@UniqueConstraint(columnNames = {"role"})})
@javax.persistence.Entity
public class RoleInfo extends AbstractBaseEntity {
    public static final String TABLE_NAME = "jarboot_role";
    private String role;
    private String name;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
