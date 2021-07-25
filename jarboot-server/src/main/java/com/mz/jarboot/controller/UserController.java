package com.mz.jarboot.controller;

import com.mz.jarboot.auth.annotation.Permission;
import com.mz.jarboot.common.ResponseForList;
import com.mz.jarboot.common.ResponseForObject;
import com.mz.jarboot.common.ResponseSimple;
import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.constant.AuthConst;
import com.mz.jarboot.entity.User;
import com.mz.jarboot.security.JwtTokenManager;
import com.mz.jarboot.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户管理
 * @author majianzheng
 */
@RequestMapping(value = "/api/jarboot/user")
@RestController
@Permission
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtTokenManager jwtTokenManager;

    /**
     * 创建用户
     * @param username 用户名
     * @param password 密码
     * @return 执行结果
     */
    @PostMapping
    @ResponseBody
    @Permission
    public ResponseSimple createUser(String username, String password) {
        userService.createUser(username, password);
        return new ResponseSimple();
    }

    /**
     * 删除用户
     * @param id 用户id
     * @return 执行结果
     */
    @DeleteMapping
    @ResponseBody
    @Permission
    public ResponseSimple deleteUser(Long id) {
        userService.deleteUser(id);
        return new ResponseSimple();
    }

    /**
     * 修改密码
     * @param username 用户名
     * @param password 密码
     * @param request  http请求
     * @return 执行结果
     */
    @PutMapping
    @ResponseBody
    @Permission
    public ResponseSimple updateUserPassword(String username, String password, HttpServletRequest request) {
        String currentLoginUser = getCurrentLoginName(request);
        ResponseSimple result = new ResponseSimple();
        //只有ADMIN和自己可修改
        if (AuthConst.JARBOOT_USER.equals(currentLoginUser) || StringUtils.equals(username, currentLoginUser)) {
            userService.updateUserPassword(username, password);
        } else {
            result.setResultCode(ResultCodeConst.VALIDATE_FAILED);
            result.setResultMsg("Only ROLE_ADMIN or self can modify the password!");
        }
        return result;
    }

    /**
     * 根据用户名获取用户信息
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping
    @ResponseBody
    @Permission
    public ResponseForObject<User> findUserByUsername(String username) {
        User user = userService.findUserByUsername(username);
        return new ResponseForObject<>(user);
    }

    /**
     * 获取用户列表
     * @param pageNo   页数
     * @param pageSize 页大小
     * @return 用户列表
     */
    @GetMapping(value="/getUsers")
    @ResponseBody
    public ResponseForList<User> getUsers(int pageNo, int pageSize) {
        return userService.getUsers(pageNo, pageSize);
    }

    private String getCurrentLoginName(HttpServletRequest request) {
        String token = request.getHeader(AuthConst.AUTHORIZATION_HEADER);
        if (StringUtils.isNotBlank(token) && token.startsWith(AuthConst.TOKEN_PREFIX)) {
            token =  token.substring(7);
        }
        Authentication authentication = jwtTokenManager.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication.getName();
    }
}
