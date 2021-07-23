package com.mz.jarboot.entity;

import javax.persistence.*;

/**
 * @author jianzhengma
 */
@Table(name = User.TABLE_NAME, uniqueConstraints = {@UniqueConstraint(columnNames = {"username"})})
@Entity
public class User extends AbstractBaseEntity {
    public static final String TABLE_NAME = "jarboot_user";
    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String userName) {
        this.username = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
