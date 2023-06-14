package com.mz.jarboot.dao;

import com.mz.jarboot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author majianzheng
 */
@Repository
public interface UserDao extends JpaRepository<User, Long> {

    /**
     * 获取用户信息
     * @param username 用户
     * @return 用户信息
     */
    User findFirstByUsername(String username);

    /**
     * 根据角色用户信息
     * @param roles 角色
     * @return
     */
    List<User> findAllByRolesLike(String roles);

    /**
     * 获取用户角色
     * @param username 用户名
     * @return 用户角色
     */
    @Query("select u.roles from User u where u.username=:username")
    String getUserRoles(@Param("username") String username);
}
