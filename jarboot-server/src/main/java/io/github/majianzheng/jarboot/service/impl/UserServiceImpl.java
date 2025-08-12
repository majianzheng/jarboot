package io.github.majianzheng.jarboot.service.impl;

import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.pojo.PagedList;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.constant.AuthConst;
import io.github.majianzheng.jarboot.dao.PrivilegeDao;
import io.github.majianzheng.jarboot.dao.RoleDao;
import io.github.majianzheng.jarboot.dao.UserDao;
import io.github.majianzheng.jarboot.entity.Privilege;
import io.github.majianzheng.jarboot.entity.User;
import io.github.majianzheng.jarboot.service.UserService;
import io.github.majianzheng.jarboot.utils.CommonUtils;
import io.github.majianzheng.jarboot.utils.PasswordEncoderUtil;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author majianzheng
 */
@Service
public class UserServiceImpl implements UserService {
    private static final String PATTEN = "^[A-Za-z_0-9\u4E00-\u9FA5]{3,18}$";
    private static final int FULL_NAME_MAX = 26;
    private static final int PASSWORD_MAX = 17;
    private static final int PASSWORD_MIN = 5;
    @Resource
    private UserDao userDao;
    @Resource
    private RoleDao roleDao;
    @Resource
    private PrivilegeDao privilegeDao;

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void createUser(String username, String fullName, String password, String roles, String userDir, String avatar) {
        checkPassword(password);
        if (AuthConst.JARBOOT_USER.equalsIgnoreCase(username)) {
            throw new JarbootException("Create user:" + username + " is internal user!");
        }
        checkFullName(fullName);
        checkUsernameAndRoles(username, roles);
        User user = new User();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setRoles(roles);
        if (StringUtils.isEmpty(userDir)) {
            user.setUserDir(username);
        } else {
            if (StringUtils.containsWhitespace(userDir)) {
                throw new JarbootException("User dir:" + userDir + " contains whitespace!");
            }
            if (!Pattern.matches(PATTEN, userDir)) {
                throw new JarbootException("userDir must be A-Z a-z _ 0-9 length 3 to 18");
            }
            user.setUserDir(userDir);
        }
        user.setPassword(PasswordEncoderUtil.encode(password));
        if (StringUtils.isNotEmpty(avatar)) {
            writeAvatar(username, avatar);
        }
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
            throw new JarbootException("Can't find the user to delete.");
        }
        if (AuthConst.JARBOOT_USER.equals(user.getUsername())) {
            throw new JarbootException("The internal user, can't removed!");
        }
        userDao.delete(user);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void updateUserPassword(String currentLoginUser, String username, String oldPassword, String password) {
        checkPassword(password);
        if (StringUtils.isEmpty(currentLoginUser) || StringUtils.isEmpty(username)) {
            throw new JarbootException("User or password is empty!");
        }
        User user = userDao.findFirstByUsername(username);
        if (null == user) {
            if (AuthConst.JARBOOT_USER.equalsIgnoreCase(username)) {
                user = new User();
                user.setUsername(AuthConst.JARBOOT_USER);
            } else {
                throw new JarbootException(String.format("User:%s is not exist!", username));
            }
        }
        if (!AuthConst.JARBOOT_USER.equals(currentLoginUser) && !PasswordEncoderUtil.matches(oldPassword, user.getPassword())) {
            throw new JarbootException("Password or username is not correct!");
        }
        user.setPassword(PasswordEncoderUtil.encode(password));
        userDao.save(user);
    }

    @Override
    public void updateUser(String username, String fullName, String roles, String userDir, String avatar) {
        checkUsername(username);
        checkFullName(fullName);
        User user = userDao.findFirstByUsername(username);
        if (null == user) {
            throw new JarbootException("User:" + username + " is not exist!");
        }
        // 1、非super账号不可赋予SYS角色
        // 2、super账号禁止取消SYS角色
        // 3、登录账号非管理员时，修改账号角色只能为普通账号
        checkUsernameAndRoles(username, roles);
        Set<String> loginRoles = CommonUtils.getLoginRoles();
        if (loginRoles.contains(AuthConst.SYS_ROLE)) {
            user.setRoles(roles);
        } else {
            // 登录账号非管理员，修改的账号角色含义管理员时
            if (roles.contains(AuthConst.SYS_ROLE)) {
                throw new JarbootException("普通账号不允许提升权限！");
            }
            user.setRoles(roles);
        }
        if (StringUtils.isNotEmpty(roles)) {
            checkUsernameAndRoles(username, roles);
            user.setRoles(roles);
        }
        if (StringUtils.isNotEmpty(userDir)) {
            user.setUserDir(userDir);
        }
        user.setFullName(fullName);
        if (StringUtils.isNotEmpty(avatar)) {
            writeAvatar(username, avatar);
        }
        userDao.save(user);
    }

    private void checkFullName(String fullName) {
        if (StringUtils.isEmpty(fullName)) {
            return;
        }
        if (StringUtils.isNotEmpty(fullName) && fullName.length() > FULL_NAME_MAX) {
            throw new JarbootException("Full name length is big than 26!");
        }
        if (fullName.contains(StringUtils.LF) || fullName.contains(StringUtils.CR)) {
            throw new JarbootException("Full name can't contains LF or CR!");
        }
    }

    @Override
    public User findUserByUsername(String username) {
        if (StringUtils.isEmpty(username)) {
            throw new JarbootException("User name can't be empty!");
        }
        User user = userDao.findFirstByUsername(username);
        if (null == user && AuthConst.JARBOOT_USER.equalsIgnoreCase(username)) {
            // 内置的用户名
            user = new User();
            // 使用默认的用户名和密码
            user.setUsername(AuthConst.JARBOOT_USER);
            user.setRoles(AuthConst.ADMIN_ROLE);
            user.setPassword(PasswordEncoderUtil.encode(AuthConst.JARBOOT_USER));
        }
        if (null != user) {
            Set<String> roleSet = Arrays.stream(user.getRoles().split(",")).map(String::trim).collect(Collectors.toSet());
            List<Privilege> list = privilegeDao.findAllByRoleIn(roleSet);
            Map<String, Boolean> map = new HashMap<>(16);
            list.forEach(p -> map.compute(p.getAuthCode(), (k, v) -> {
                boolean val = Boolean.TRUE.equals(p.getPermission());
                if (null == v) {
                    return val;
                }
                return Boolean.TRUE.equals(v) || val;
            }));
            user.setPrivileges(map);
        }
        return user;
    }

    @Override
    public PagedList<User> getUsers(String username, String role, int pageNo, int pageSize) {
        PageRequest page = PageRequest.of(pageNo, pageSize);
        ExampleMatcher match = ExampleMatcher.matching().withStringMatcher(ExampleMatcher.StringMatcher.STARTING);
        if (StringUtils.isNotEmpty(username)) {
            match = match.withMatcher("username", ExampleMatcher.GenericPropertyMatchers.contains());
        }
        if (StringUtils.isNotEmpty(role)) {
            match = match.withMatcher("roles", ExampleMatcher.GenericPropertyMatchers.contains());
        }
        Page<User> all;
        if (StringUtils.isNotEmpty(username) || StringUtils.isNotEmpty(role)) {
            match = match.withIgnorePaths("password", "userDir");
            User query = new User();
            query.setUsername(username);
            query.setRoles(role);
            all = userDao.findAll(Example.of(query, match), page);
        } else {
            all = userDao.findAll(page);
        }
        List<User> result = all.getContent();
        return new PagedList<>(result, all.getTotalElements());
    }

    private void writeAvatar(String username, String avatar) {
        try {
            FileUtils.writeStringToFile(getUserAvatarFile(username), avatar, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new JarbootException(e.getMessage(), e);
        }
    }

    @Override
    public String getAvatar(String username) {
        File avatarFile = getUserAvatarFile(username);
        if (!avatarFile.exists()) {
            return StringUtils.EMPTY;
        }
        try {
            return FileUtils.readFileToString(avatarFile, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new JarbootException(e.getMessage(), e);
        }
    }

    private File getUserAvatarFile(String username) {
        File avatarDir = FileUtils.getFile(SettingUtils.getHomePath(), "data", "avatar");
        if (!avatarDir.exists()) {
            try {
                FileUtils.forceMkdir(avatarDir);
            } catch (IOException e) {
                throw new JarbootException(e.getMessage(), e);
            }
        }
        return FileUtils.getFile(avatarDir, username);
    }

    private void checkUsername(String username) {
        if (StringUtils.isEmpty(username)) {
            throw new JarbootException("Username is empty!");
        }
        if (StringUtils.containsWhitespace(username)) {
            throw new JarbootException("User:" + username + " contains whitespace!");
        }
        if (!Pattern.matches(PATTEN, username)) {
            throw new JarbootException("Username must be A-Z a-z _ 0-9 length 3 to 18");
        }
    }

    private void checkUsernameAndRoles(String username, String roles) {
        checkUsername(username);
        if (StringUtils.isEmpty(roles)) {
            throw new JarbootException("Role is empty!");
        }
        if (StringUtils.containsWhitespace(roles)) {
            throw new JarbootException("Role contains whitespace!");
        }
        if (AuthConst.JARBOOT_USER.equals(username) && !roles.contains(AuthConst.SYS_ROLE)) {
            // 禁止为super账号降级
            throw new JarbootException("禁止为super账号取消超级管理员角色!");
        }
        Set<String> roleSet = Arrays.stream(roles.split(",")).map(String::trim).collect(Collectors.toSet());
        if (roleSet.contains(AuthConst.CLUSTER_ROLE)) {
            throw new JarbootException("Can not assign CLUSTER role to any user!");
        }
        if (roleSet.contains(AuthConst.SYS_ROLE) && !AuthConst.JARBOOT_USER.equals(username)) {
            throw new JarbootException("Can not assign SYSTEM role to normal user!");
        }
        for (String role : roleSet) {
            if (!roleDao.existsByRole(role)) {
                throw new JarbootException("Role is not exist!");
            }
        }
    }

    private void checkPassword(String password) {
        if (StringUtils.isEmpty(password)) {
            throw new JarbootException("Password is empty!");
        }
        if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            throw new JarbootException("Password length range 5-17!");
        }
    }

    @Transactional(rollbackFor = Throwable.class)
    @PostConstruct
    public void init() {
        String username = System.getenv("JARBOOT_USER");
        if (StringUtils.isEmpty(username)) {
            username = AuthConst.JARBOOT_USER;
        }
        //检查是否存在jarboot用户，否则创建
        User user = userDao.findFirstByUsername(username);
        if (null != user) {
            return;
        }
        user = new User();

        user.setUsername(username);
        user.setRoles(AuthConst.SYS_ROLE + "," + AuthConst.ADMIN_ROLE);
        user.setUserDir("default");
        String defaultPwd = System.getenv("JARBOOT_DEFAULT_PWD");
        if (StringUtils.isEmpty(defaultPwd)) {
            defaultPwd = AuthConst.JARBOOT_USER;
        }
        user.setPassword(PasswordEncoderUtil.encode(defaultPwd));
        userDao.save(user);
    }
}
