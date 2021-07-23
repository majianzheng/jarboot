package com.mz.jarboot.dao;

import com.mz.jarboot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author jianzhengma
 */
@Repository
public interface UserDao extends JpaRepository<User, Long> {

    /**
     * 获取用户信息
     * @param username 用户
     * @return 用户信息
     */
    User findFirstByUsername(String username);
}
