package com.mz.jarboot.service.impl;

import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.common.pojo.PagedList;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.constant.AuthConst;
import com.mz.jarboot.dao.PrivilegeDao;
import com.mz.jarboot.dao.RoleDao;
import com.mz.jarboot.entity.RoleInfo;
import com.mz.jarboot.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author majianzheng
 */
@Service
public class RoleServiceImpl implements RoleService {
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private PrivilegeDao privilegeDao;

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
        if (StringUtils.isEmpty(role) || StringUtils.isEmpty(name)) {
            throw new JarbootException("Argument can't be empty！");
        }
        if (!role.startsWith(AuthConst.ROLE_PREFIX)) {
            throw new JarbootException("Role must start with \"ROLE_\"！");
        }
        if (AuthConst.ADMIN_ROLE.equalsIgnoreCase(role)) {
            throw new JarbootException("Role Admin is not permit to create！");
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
        if (StringUtils.isEmpty(role) || StringUtils.isEmpty(name)) {
            throw new JarbootException("Argument can't be empty！");
        }
        if (!role.startsWith(AuthConst.ROLE_PREFIX)) {
            throw new JarbootException("Role must start with \"ROLE_\"！");
        }
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
        if (AuthConst.ADMIN_ROLE.equals(role)) {
            throw new JarbootException("The internal role, can't delete.");
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

    @Transactional(rollbackFor = Throwable.class)
    @PostConstruct
    public void init() {
        //检查是否存在ADMIN_ROLE，否则创建
        RoleInfo roleInfo = roleDao.findFirstByRole(AuthConst.ADMIN_ROLE);
        if (null != roleInfo) {
            return;
        }
        roleInfo = new RoleInfo();
        roleInfo.setName("Admin");
        roleInfo.setRole(AuthConst.ADMIN_ROLE);
        roleDao.save(roleInfo);
    }
}
