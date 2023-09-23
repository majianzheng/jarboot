package io.github.majianzheng.jarboot.controller;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.common.pojo.PagedList;
import io.github.majianzheng.jarboot.common.pojo.ResponseVo;
import io.github.majianzheng.jarboot.common.pojo.ResponseSimple;
import io.github.majianzheng.jarboot.common.pojo.ResultCodeConst;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.constant.AuthConst;
import io.github.majianzheng.jarboot.entity.User;
import io.github.majianzheng.jarboot.security.JwtTokenManager;
import io.github.majianzheng.jarboot.service.UserService;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        return HttpResponseUtils.success();
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
        return HttpResponseUtils.success();
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
        return HttpResponseUtils.success();
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
        //只有ADMIN和自己可修改
        if (AuthConst.JARBOOT_USER.equals(currentLoginUser) || Objects.equals(username, currentLoginUser)) {
            userService.updateUserPassword(currentLoginUser, username, oldPassword, password);
        } else {
            return HttpResponseUtils.error(ResultCodeConst.VALIDATE_FAILED, "Only ROLE_ADMIN or self can modify the password!");
        }
        return HttpResponseUtils.success();
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
        return HttpResponseUtils.success(user);
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
        List<String> userDirs = Stream.of(Objects.requireNonNull(workspace.listFiles()))
                .filter(file -> file.isDirectory() && !file.getName().startsWith(".") && !file.isHidden())
                .map(File::getName)
                .collect(Collectors.toList());
        return HttpResponseUtils.success(userDirs);
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

    /**
     * 获取头像
     * @param username 用户名
     * @return 头像
     */
    @GetMapping(value="/avatar")
    public ResponseVo<String> getAvatar(String username) {
        return HttpResponseUtils.success(userService.getAvatar(username));
    }
}
