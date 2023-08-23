package io.github.majianzheng.jarboot.service;

import io.github.majianzheng.jarboot.entity.Privilege;

import java.util.List;

/**
 * 权限服务
 * @author majianzheng
 */
public interface PrivilegeService {
    /**
     * 获取角色的权限列表
     * @param role 角色
     * @return 权限列表
     */
    List<Privilege> getPrivilegeByRole(String role);

    /**
     * 保持权限
     * @param role 角色
     * @param authCode 权限
     * @param permission 是否拥有权限
     */
    void savePrivilege(String role, String authCode, Boolean permission);

    /**
     * 判断角色是否拥有权限
     * @param role 角色
     * @param authCode 权限
     * @return 是否有权限
     */
    boolean hasPrivilege(String role, String authCode);
}
