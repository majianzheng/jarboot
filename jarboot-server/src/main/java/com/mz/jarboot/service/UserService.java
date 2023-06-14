package com.mz.jarboot.service;

import com.mz.jarboot.common.pojo.PagedList;
import com.mz.jarboot.entity.User;

/**
 * @author majianzheng
 */
public interface UserService {
    /**
     * 创建用户
     * @param username 用户名
     * @param fullName 姓名
     * @param password 密码
     * @param roles 角色
     * @param userDir 用户关联目录
     */
    void createUser(String username, String fullName, String password, String roles, String userDir);

    /**
     * 删除用户
     * @param id 用户ID
     */
    void deleteUser(Long id);

    /**
     * 更新密码
     * @param loginUsername 当前登陆的用户
     * @param username 用户
     * @param oldPassword 旧密码
     * @param password 密码
     */
    void updateUserPassword(String loginUsername, String username, String oldPassword, String password);

    /**
     * 更新用户信息
     * @param username 用户
     * @param fullName 姓名
     * @param roles 角色
     * @param userDir 用户目录
     */
    void updateUser(String username, String fullName, String roles, String userDir);

    /**
     * 根据用户名获取用户信息
     * @param username 用户名
     * @return 用户信息
     */
    User findUserByUsername(String username);

    /**
     * 获取用户列表
     * @param username 用户名
     * @param role 角色
     * @param pageNo 开始页
     * @param pageSize 页大小
     * @return 用户列表
     */
    PagedList<User> getUsers(String username, String role, int pageNo, int pageSize);
}
