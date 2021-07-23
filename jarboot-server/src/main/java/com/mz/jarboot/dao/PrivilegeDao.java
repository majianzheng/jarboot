package com.mz.jarboot.dao;

import com.mz.jarboot.entity.Privilege;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author jianzhengma
 */
@Repository
public interface PrivilegeDao extends JpaRepository<Privilege, Long> {

    /**
     * 根据角色获取所有权限信息
     * @param role 角色
     * @return 权限信息
     */
    List<Privilege> findAllByRole(String role);

    /**
     * 获取角色对某一资源的权限
     * @param role 角色
     * @param resource 资源
     * @return 权限信息
     */
    Privilege findFirstByRoleAndResource(String role, String resource);

    /**
     * 根据角色删除所有权限信息
     * @param role 角色
     */
    @Modifying
    void deleteAllByRole(String role);
}
