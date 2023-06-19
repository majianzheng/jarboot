package com.mz.jarboot.controller;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.common.pojo.ResponseVo;
import com.mz.jarboot.common.pojo.ResultCodeConst;
import com.mz.jarboot.common.utils.HttpResponseUtils;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.constant.AuthConst;
import com.mz.jarboot.dao.UserDao;
import com.mz.jarboot.entity.User;
import com.mz.jarboot.security.JarbootUser;
import com.mz.jarboot.security.JwtTokenManager;
import com.mz.jarboot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 鉴权接口
 * @author majianzheng
 */
@RequestMapping(value = CommonConst.AUTH_CONTEXT)
@Controller
public class AuthController {
    private static final String PARAM_USERNAME = "username";

    private static final String PARAM_PASSWORD = "password";

    @Autowired
    private JwtTokenManager jwtTokenManager;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserService userService;

    @Value("${jarboot.token.expire.seconds:7776000}")
    private long expireSeconds;

    /**
     * 获取当前登录的用户
     * @param request Http请求
     * @return 结果
     */
    @GetMapping(value="/getCurrentUser")
    @ResponseBody
    public ResponseVo<User> getCurrentUser(HttpServletRequest request) {
        String token;
        try {
            token = resolveToken(request);
            if (StringUtils.isEmpty(token)) {
                return HttpResponseUtils.error(ResultCodeConst.NOT_LOGIN_ERROR, "当前未登录");
            }
        } catch (Exception e) {
            return HttpResponseUtils.error(ResultCodeConst.NOT_LOGIN_ERROR, "当前未登录: " + e.getMessage());
        }

        Authentication authentication = jwtTokenManager.getAuthentication(token);
        User user = userService.findUserByUsername(authentication.getName());
        user.setPassword(null);
        return HttpResponseUtils.success(user);
    }

    /**
     * 登入系统
     * @param request http请求
     * @return 结果
     */
    @PostMapping(value="/login")
    @ResponseBody
    public ResponseVo<JarbootUser> login(HttpServletRequest request) {
        String token = getToken(request);
        String username = request.getParameter(PARAM_USERNAME);
        if (StringUtils.isEmpty(username) && !StringUtils.isBlank(token)) {
            // 已经登录了，鉴定权限
            jwtTokenManager.validateToken(token);
            Authentication authentication = jwtTokenManager.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            username = authentication.getName();
        } else {
            try {
                String password = request.getParameter(PARAM_PASSWORD);
                token = resolveTokenFromUser(username, password);
            } catch (Exception e) {
                return HttpResponseUtils.error(e.getMessage(), e);
            }
        }
        User user = userService.findUserByUsername(username);
        JarbootUser jarbootUser = new JarbootUser();
        jarbootUser.setUsername(username);
        jarbootUser.setAccessToken(token);
        jarbootUser.setTokenTtl(expireSeconds);
        jarbootUser.setRoles(user.getRoles());
        jarbootUser.setAvatar(userService.getAvatar(username));
        return HttpResponseUtils.success(jarbootUser);
    }

    /**
     * 创建Open Api的访问Token
     * @param username 用户
     * @param password 密码
     * @return token
     */
    @PostMapping(value="/openApiToken")
    @ResponseBody
    public ResponseVo<String> createOpenApiToken(String username, String password) {
        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username,
                    password);
            authenticationManager.authenticate(authenticationToken);
        } catch (Exception e) {
            throw new JarbootException(e.getMessage(), e);
        }
        String token = jwtTokenManager.createOpenApiToken(username);
        return HttpResponseUtils.success(token);
    }

    private String getToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AuthConst.AUTHORIZATION_HEADER);
        if (!StringUtils.isBlank(bearerToken) && bearerToken.startsWith(AuthConst.TOKEN_PREFIX)) {
            return bearerToken.substring(7);
        }
        return request.getParameter(AuthConst.ACCESS_TOKEN);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = getToken(request);
        if (StringUtils.isBlank(bearerToken)) {
            return StringUtils.EMPTY;
        }
        return bearerToken;
    }

    private String resolveTokenFromUser(String userName, String rawPassword) {
        String finalName;
        Authentication authenticate;
        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userName,
                    rawPassword);
            authenticate = authenticationManager.authenticate(authenticationToken);

            SecurityContextHolder.getContext().setAuthentication(authenticate);
        } catch (AuthenticationException e) {
            throw new JarbootException(e.getMessage(), e);
        }

        if (null == authenticate || StringUtils.isBlank(authenticate.getName())) {
            finalName = userName;
        } else {
            finalName = authenticate.getName();
        }
        return jwtTokenManager.createToken(finalName);
    }
}
