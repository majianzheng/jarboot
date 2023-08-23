package io.github.majianzheng.jarboot.service.impl;

import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.pojo.PagedList;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.constant.AuthConst;
import io.github.majianzheng.jarboot.dao.PrivilegeDao;
import io.github.majianzheng.jarboot.dao.RoleDao;
import io.github.majianzheng.jarboot.dao.UserDao;
import io.github.majianzheng.jarboot.entity.RoleInfo;
import io.github.majianzheng.jarboot.entity.User;
import io.github.majianzheng.jarboot.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author majianzheng
 */
@Service
public class RoleServiceImpl implements RoleService {
    private static final String PATTEN = "^ROLE_[A-Z0-9]{1,10}$";
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private PrivilegeDao privilegeDao;
    @Autowired
    private UserDao userDao;

    @Override
    public PagedList<RoleInfo> getRoles(String role, String name, int pageNo, int pageSize) {
        PageRequest page = PageRequest.of(pageNo, pageSize);
        ExampleMatcher match = ExampleMatcher.matching().withStringMatcher(ExampleMatcher.StringMatcher.STARTING);
        if (StringUtils.isNotEmpty(role)) {
            match = match.withMatcher("role", ExampleMatcher.GenericPropertyMatchers.contains());
        }
        if (StringUtils.isNotEmpty(name)) {
            match = match.withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains());
        }
        Page<RoleInfo> all;
        if (StringUtils.isNotEmpty(role) || StringUtils.isNotEmpty(name)) {
            RoleInfo query = new RoleInfo();
            query.setName(name);
            query.setRole(role);
            all = roleDao.findAll(Example.of(query, match), page);
        } else {
            all = roleDao.findAll(page);
        }
        return new PagedList<>(all.getContent(), all.getTotalElements());
    }


    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void addRole(String role, String name) {
        checkParam(role, name);
        if (AuthConst.ADMIN_ROLE.equalsIgnoreCase(role)) {
            throw new JarbootException("Role ADMIN is not permit to create!");
        }
        if (AuthConst.SYS_ROLE.equalsIgnoreCase(role)) {
            throw new JarbootException("Role SYSTEM is not permit to create!");
        }
        if (AuthConst.CLUSTER_ROLE.equalsIgnoreCase(role)) {
            throw new JarbootException("Role CLUSTER is not permit to create!");
        }
        if (null == roleDao.findFirstByRole(role) && roleDao.countRoles() > AuthConst.MAX_ROLE) {
            throw new JarbootException("Role number exceed " + AuthConst.MAX_ROLE + "!");
        }
        RoleInfo r = new RoleInfo();
        r.setRole(role);
        r.setName(name);
        roleDao.save(r);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void setRoleName(String role, String name) {
        checkParam(role, name);
        RoleInfo r = roleDao.findFirstByRole(role);
        r.setName(name);
        roleDao.save(r);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteRole(String role) {
        if (StringUtils.isEmpty(role)) {
            throw new JarbootException("Argument role can't be empty！");
        }
        if (AuthConst.ADMIN_ROLE.equals(role) || AuthConst.SYS_ROLE.equals(role)) {
            throw new JarbootException("The internal role, can't delete.");
        }
        List<User> userList = userDao.findAllByRolesLike(role);
        if (null != userList && !userList.isEmpty()) {
            for (User user : userList) {
                Set<String> roleSet = Arrays.stream(user.getRoles().split(",")).map(String::trim).collect(Collectors.toSet());
                if (roleSet.contains(role)) {
                    throw new JarbootException("Role is using by " + user.getUsername());
                }
            }
        }
        roleDao.deleteAllByRole(role);
        // 当前role已经没有任何关联的user，删除相关的权限
        privilegeDao.deleteAllByRole(role);
    }

    @Override
    public List<String> findRolesLikeRoleName(String role) {
        return roleDao.findRolesLikeRoleName(role);
    }

    @Override
    public List<RoleInfo> getRoleList() {
        return roleDao.findAll();
    }

    private void checkParam(String role, String name) {
        if (StringUtils.isEmpty(role) || StringUtils.isEmpty(name)) {
            throw new JarbootException("Argument can't be empty！");
        }
        if (!Pattern.matches(PATTEN, role)) {
            throw new JarbootException("Role is not legal, must start with \"ROLE_\" and A-Z 0-9！");
        }
    }

    @Transactional(rollbackFor = Throwable.class)
    @PostConstruct
    public void init() {
        //检查是否存在，否则创建
        RoleInfo roleInfo = roleDao.findFirstByRole(AuthConst.SYS_ROLE);
        if (null != roleInfo) {
            return;
        }
        roleInfo = new RoleInfo();
        roleInfo.setName("SYSTEM");
        roleInfo.setRole(AuthConst.SYS_ROLE);
        roleDao.save(roleInfo);
        roleInfo = roleDao.findFirstByRole(AuthConst.ADMIN_ROLE);
        if (null != roleInfo) {
            return;
        }
        roleInfo = new RoleInfo();
        roleInfo.setName("ADMIN");
        roleInfo.setRole(AuthConst.ADMIN_ROLE);
        roleDao.save(roleInfo);
    }
}
