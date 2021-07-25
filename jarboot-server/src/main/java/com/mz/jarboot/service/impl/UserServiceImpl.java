package com.mz.jarboot.service.impl;

import com.mz.jarboot.common.MzException;
import com.mz.jarboot.common.ResponseForList;
import com.mz.jarboot.constant.AuthConst;
import com.mz.jarboot.dao.PrivilegeDao;
import com.mz.jarboot.dao.RoleDao;
import com.mz.jarboot.dao.UserDao;
import com.mz.jarboot.entity.RoleInfo;
import com.mz.jarboot.entity.User;
import com.mz.jarboot.service.UserService;
import com.mz.jarboot.utils.PasswordEncoderUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author majianzheng
 */
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserDao userDao;
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private PrivilegeDao privilegeDao;

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void createUser(String username, String password) {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            throw new IllegalArgumentException("User or password is empty!");
        }
        if (AuthConst.JARBOOT_USER.equalsIgnoreCase(username)) {
            throw new IllegalArgumentException("User:" + username + " is internal user!");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordEncoderUtil.encode(password));
        userDao.save(user);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteUser(Long id) {
        if (null == id) {
            throw new NullPointerException("id is empty!");
        }
        User user = userDao.findById(id).orElse(null);
        if (null == user) {
            throw new MzException("Can't find the user to delete.");
        }
        if (AuthConst.JARBOOT_USER.equals(user.getUsername())) {
            throw new MzException("The internal user, can't removed!");
        }
        userDao.delete(user);
        Page<RoleInfo> roles = roleDao.getRoleByUsername(user.getUsername(), PageRequest.of(0, Integer.MAX_VALUE));
        if (null == roles || roles.isEmpty()) {
            return;
        }
        List<RoleInfo> list = roles.getContent();
        roleDao.deleteAll(list);
        list.forEach(roleInfo -> {
            if (null == roleDao.findFirstByRole(roleInfo.getRole())) {
                // 当前role已经没有任何关联的user，删除相关的权限
                privilegeDao.deleteAllByRole(roleInfo.getRole());
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void updateUserPassword(String username, String password) {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            throw new IllegalArgumentException("User or password is empty!");
        }
        User user = userDao.findFirstByUsername(username);
        if (null == user) {
            if (AuthConst.JARBOOT_USER.equalsIgnoreCase(username)) {
                user = new User();
                user.setUsername(AuthConst.JARBOOT_USER);
            } else {
                throw new NoSuchElementException("User:" + username + " is not exist!");
            }
        }
        user.setPassword(PasswordEncoderUtil.encode(password));
        userDao.save(user);
    }

    @Override
    public User findUserByUsername(String username) {
        if (StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException("User name can't be empty!");
        }
        User user = userDao.findFirstByUsername(username);
        if (null == user && AuthConst.JARBOOT_USER.equalsIgnoreCase(username)) {
            // 内置的用户名
            user = new User();
            // 使用默认的用户名和密码
            user.setUsername(AuthConst.JARBOOT_USER);
            user.setPassword(PasswordEncoderUtil.encode(AuthConst.JARBOOT_USER));
        }
        return user;
    }

    @Override
    public ResponseForList<User> getUsers(int pageNo, int pageSize) {
        PageRequest page = PageRequest.of(pageNo, pageSize);
        Page<User> all = userDao.findAll(page);
        List<User> result = all.getContent();
        return new ResponseForList<>(result, all.getTotalElements());
    }

    @Transactional(rollbackFor = Throwable.class)
    @PostConstruct
    public void init() {
        //检查是否存在jarboot用户，否则创建
        User user = userDao.findFirstByUsername(AuthConst.JARBOOT_USER);
        if (null != user) {
            return;
        }
        user = new User();
        user.setUsername(AuthConst.JARBOOT_USER);
        user.setPassword(PasswordEncoderUtil.encode(AuthConst.JARBOOT_USER));
        userDao.save(user);
    }
}
