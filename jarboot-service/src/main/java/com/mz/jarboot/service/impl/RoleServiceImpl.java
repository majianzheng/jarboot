package com.mz.jarboot.service.impl;

import com.mz.jarboot.common.ResponseForList;
import com.mz.jarboot.constant.AuthConst;
import com.mz.jarboot.dao.RoleDao;
import com.mz.jarboot.dao.UserDao;
import com.mz.jarboot.entity.RoleInfo;
import com.mz.jarboot.entity.User;
import com.mz.jarboot.service.RoleService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private UserDao userDao;
    @Override
    public ResponseForList<RoleInfo> getRoles(int pageNo, int pageSize) {
        PageRequest page = PageRequest.of(pageNo, pageSize);
        Page<RoleInfo> all = roleDao.findAll(page);
        return new ResponseForList<>(all.getContent(), all.getTotalElements());
    }

    @Override
    public ResponseForList<RoleInfo> getRolesByUserName(String username, int pageNo, int pageSize) {
        Page<RoleInfo> page = roleDao.getRoleByUsername(username, PageRequest.of(pageNo, pageSize));
        return new ResponseForList<>(page.getContent(), page.getTotalElements());
    }

    @Override
    @Transactional
    public void addRole(String role, String username) {
        if (StringUtils.isEmpty(role) || StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException("Argument can't be empty！");
        }
        if (AuthConst.ADMIN_ROLE.equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("Role Admin is not permit to create！");
        }
        User user = userDao.findFirstByUsername(username);
        if (null == user) {
            throw new IllegalArgumentException("User name is not exist！");
        }
        if (null == roleDao.findFirstByRole(role) && roleDao.countRoles() > AuthConst.MAX_ROLE) {
            throw new IllegalArgumentException("Role number exceed " + AuthConst.MAX_ROLE + "!");
        }
        RoleInfo r = new RoleInfo();
        r.setRole(role);
        r.setUsername(username);
        roleDao.save(r);
    }

    @Override
    @Transactional
    public void deleteRole(String role) {
        if (StringUtils.isEmpty(role)) {
            throw new IllegalArgumentException("Argument role can't be empty！");
        }
        if (AuthConst.ADMIN_ROLE.equals(role)) {
            throw new IllegalArgumentException("The internal role, can't delete.");
        }
        roleDao.deleteAllByRole(role);
    }

    @Override
    @Transactional
    public void deleteRole(String role, String username) {
        if (StringUtils.isEmpty(role) || StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException("Argument role or name can't be empty！");
        }
        if (AuthConst.ADMIN_ROLE.equals(role) && AuthConst.JARBOOT_USER.equals(username)) {
            throw new IllegalArgumentException("The internal role, can't delete.");
        }
        roleDao.deleteByRoleAndUsername(role, username);
    }

    @Override
    public List<String> findRolesLikeRoleName(String role) {
        return roleDao.findRolesLikeRoleName(role);
    }

    @Override
    public List<String> getRoleList() {
        return roleDao.getRoleList();
    }

    @Transactional
    @PostConstruct
    public void init() {
        //检查是否存在ADMIN_ROLE，否则创建
        RoleInfo roleInfo = roleDao.findFirstByRoleAndUsername(AuthConst.ADMIN_ROLE, AuthConst.JARBOOT_USER);
        if (null != roleInfo) {
            return;
        }
        roleInfo = new RoleInfo();
        roleInfo.setUsername(AuthConst.JARBOOT_USER);
        roleInfo.setRole(AuthConst.ADMIN_ROLE);
        roleDao.save(roleInfo);
    }
}
