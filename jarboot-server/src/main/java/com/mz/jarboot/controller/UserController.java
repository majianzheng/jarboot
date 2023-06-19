package com.mz.jarboot.controller;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.common.pojo.PagedList;
import com.mz.jarboot.common.pojo.ResponseVo;
import com.mz.jarboot.common.pojo.ResponseSimple;
import com.mz.jarboot.common.pojo.ResultCodeConst;
import com.mz.jarboot.common.utils.HttpResponseUtils;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.constant.AuthConst;
import com.mz.jarboot.entity.User;
import com.mz.jarboot.security.JwtTokenManager;
import com.mz.jarboot.service.UserService;
import com.mz.jarboot.utils.SettingUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 用户管理
 * @author majianzheng
 */
@RequestMapping(value = CommonConst.USER_CONTEXT)
@RestController
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtTokenManager jwtTokenManager;

    /**
     * 创建用户
     * @param username 用户名
     * @param fullName 姓名
     * @param password 密码
     * @param roles 角色
     * @param userDir 用户目录
     * @param avatar 头像
     * @return 执行结果
     */
    @PostMapping
    @ResponseBody
    public ResponseSimple createUser(String username, String fullName, String password, String roles, @RequestParam(required = false) String userDir, @RequestParam(required = false) String avatar) {
        userService.createUser(username, fullName, password, roles, userDir, avatar);
        return new ResponseSimple();
    }

    /**
     * 修改用户
     * @param username 用户名
     * @param fullName 姓名
     * @param roles 角色
     * @param userDir 用户目录
     * @param avatar 头像
     * @return 执行结果
     */
    @PostMapping("/update")
    @ResponseBody
    public ResponseSimple updateUser(String username, String fullName, String roles, @RequestParam(required = false) String userDir, @RequestParam(required = false) String avatar) {
        userService.updateUser(username, fullName, roles, userDir, avatar);
        return new ResponseSimple();
    }

    /**
     * 删除用户
     * @param id 用户id
     * @return 执行结果
     */
    @DeleteMapping
    @ResponseBody
    public ResponseSimple deleteUser(Long id) {
        userService.deleteUser(id);
        return new ResponseSimple();
    }

    /**
     * 修改密码
     * @param username 用户名
     * @param oldPassword 旧密码
     * @param password 密码
     * @param request  http请求
     * @return 执行结果
     */
    @PutMapping
    @ResponseBody
    public ResponseSimple updateUserPassword(String username, String oldPassword, String password, HttpServletRequest request) {
        String currentLoginUser = getCurrentLoginName(request);
        ResponseSimple result = new ResponseSimple();
        //只有ADMIN和自己可修改
        if (AuthConst.JARBOOT_USER.equals(currentLoginUser) || java.util.Objects.equals(username, currentLoginUser)) {
            userService.updateUserPassword(currentLoginUser, username, oldPassword, password);
        } else {
            result.setCode(ResultCodeConst.VALIDATE_FAILED);
            result.setMsg("Only ROLE_ADMIN or self can modify the password!");
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
    public ResponseVo<User> findUserByUsername(String username) {
        User user = userService.findUserByUsername(username);
        return new ResponseVo<>(user);
    }

    /**
     * 获取用户列表
     * @param username 用户名
     * @param role 角色
     * @param pageNo   页数
     * @param pageSize 页大小
     * @return 用户列表
     */
    @GetMapping(value="/getUsers")
    @ResponseBody
    public ResponseVo<PagedList<User>> getUsers(String username, String role, int pageNo, int pageSize) {
        return HttpResponseUtils.success(userService.getUsers(username, role, pageNo, pageSize));
    }

    /**
     * 获取所有用户目录
     * @return 用户目录列表
     */
    @GetMapping(value="/userDirs")
    @ResponseBody
    public ResponseVo<List<String>> getUserDirs() {
        File workspace = FileUtils.getFile(SettingUtils.getWorkspace());
        return HttpResponseUtils.success(Arrays.asList(Objects.requireNonNull(workspace.list(), "工作目录为空！")));
    }

    private String getCurrentLoginName(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (null != authentication) {
            return authentication.getName();
        }
        String token = request.getHeader(AuthConst.AUTHORIZATION_HEADER);
        if (!StringUtils.isBlank(token) && token.startsWith(AuthConst.TOKEN_PREFIX)) {
            token =  token.substring(AuthConst.TOKEN_PREFIX.length());
        }
        authentication = jwtTokenManager.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication.getName();
    }

    @GetMapping(value="/avatar")
    public ResponseVo<String> getAvatar(String username) {
        return HttpResponseUtils.success(userService.getAvatar(username));
    }
}
