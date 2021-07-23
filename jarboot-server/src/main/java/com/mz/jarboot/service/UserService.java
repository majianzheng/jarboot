package com.mz.jarboot.service;

import com.mz.jarboot.common.ResponseForList;
import com.mz.jarboot.entity.User;

/**
 * @author jianzhengma
 */
public interface UserService {
    /**
     * 创建用户
     * @param username 用户名
     * @param password 密码
     */
    void createUser(String username, String password);

    /**
     * 删除用户
     * @param id 用户ID
     */
    void deleteUser(Long id);

    /**
     * 更新密码
     * @param username 用户
     * @param password 密码
     */
    void updateUserPassword(String username, String password);

    /**
     * 根据用户名获取用户信息
     * @param username 用户名
     * @return 用户信息
     */
    User findUserByUsername(String username);

    /**
     * 获取用户列表
     * @param pageNo 开始页
     * @param pageSize 页大小
     * @return 用户列表
     */
    ResponseForList<User> getUsers(int pageNo, int pageSize);
}
