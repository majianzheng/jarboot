package com.mz.jarboot.controller;

import com.mz.jarboot.common.ResponseForObject;
import com.mz.jarboot.entity.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Api(tags="鉴权接口")
@RequestMapping(value = "/api/auth", method ={RequestMethod.GET, RequestMethod.POST})
@Controller
public class OAuthController {
    @ApiOperation(value = "获取当前登录的用户", httpMethod = "GET")
    @GetMapping(value="/getCurrentUser")
    @ResponseBody
    public ResponseForObject<User> getCurrentUser(String token) {
        //暂未实现
        ResponseForObject<User> current = new ResponseForObject<>();
        User user = new User();
        user.setId(-1L);
        user.setUserName("游客");
        current.setResult(user);
        return current;
    }

    @ApiOperation(value = "登入系统", httpMethod = "POST")
    @PostMapping(value="/login")
    @ResponseBody
    public ResponseForObject<String> login(String userName, String password) {
        //暂未实现
        ResponseForObject<String> current = new ResponseForObject<>();
        current.setResult("token-test");
        return current;
    }
}
