package com.mz.jarboot.dao;

import com.mz.jarboot.entity.RoleInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleDao extends JpaRepository<RoleInfo, Long> {
    @Modifying
    void deleteAllByRole(String role);

    @Modifying
    void deleteByRoleAndUsername(String role, String username);

    @Query(value = "select r from RoleInfo r where r.username=:username")
    Page<RoleInfo> getRoleByUsername(@Param("username") String username, Pageable pageable);

    @Query("select r.role from RoleInfo r where r.role like CONCAT('%',:role,'%')")
    List<String> findRolesLikeRoleName(@Param("role") String role);

    RoleInfo findFirstByRoleAndUsername(String role, String username);

    @Query("select distinct r.role from RoleInfo r")
    List<String> getRoleList();

    RoleInfo findFirstByRole(String role);

    @Query("select count(distinct r.role) from RoleInfo r")
    long countRoles();
}
