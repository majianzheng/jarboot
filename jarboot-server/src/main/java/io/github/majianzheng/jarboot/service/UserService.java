package io.github.majianzheng.jarboot.service;

import io.github.majianzheng.jarboot.common.pojo.PagedList;
import io.github.majianzheng.jarboot.entity.User;

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
     * @param avatar 头像
     */
    void createUser(String username, String fullName, String password, String roles, String userDir, String avatar);

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
     * @param avatar 头像
     */
    void updateUser(String username, String fullName, String roles, String userDir, String avatar);

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

    /**
     * 获取头像
     * @param username 用户名
     * @return 头像
     */
    String getAvatar(String username);
}
