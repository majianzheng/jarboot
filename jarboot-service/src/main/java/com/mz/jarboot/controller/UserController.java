package com.mz.jarboot.controller;

import com.mz.jarboot.common.ResponseForList;
import com.mz.jarboot.common.ResponseForObject;
import com.mz.jarboot.common.ResponseSimple;
import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.constant.AuthConst;
import com.mz.jarboot.entity.User;
import com.mz.jarboot.security.JwtTokenManager;
import com.mz.jarboot.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Api(tags="用户管理")
@RequestMapping(value = "/api/jarboot-user", method ={RequestMethod.GET, RequestMethod.POST})
@Controller
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtTokenManager jwtTokenManager;

    @ApiOperation(value = "创建用户", httpMethod = "POST")
    @PostMapping(value="/createUser")
    @ResponseBody
    public ResponseSimple createUser(String username, String password) {
        userService.createUser(username, password);
        return new ResponseSimple();
    }

    @ApiOperation(value = "删除用户", httpMethod = "DELETE")
    @DeleteMapping(value="/deleteUser")
    @ResponseBody
    public ResponseSimple deleteUser(Long id) {
        userService.deleteUser(id);
        return new ResponseSimple();
    }

    @ApiOperation(value = "修改密码", httpMethod = "PUT")
    @PutMapping(value="/updateUserPassword")
    @ResponseBody
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

    @ApiOperation(value = "根据用户名获取用户信息", httpMethod = "GET")
    @GetMapping(value="/findUserByUsername")
    @ResponseBody
    public ResponseForObject<User> findUserByUsername(String username) {
        User user = userService.findUserByUsername(username);
        return new ResponseForObject<>(user);
    }

    @ApiOperation(value = "根据用户名获取用户信息", httpMethod = "GET")
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
