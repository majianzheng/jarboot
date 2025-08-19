package io.github.majianzheng.jarboot.controller;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.annotation.EnableAuditLog;
import io.github.majianzheng.jarboot.common.pojo.ResponseVo;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.constant.AuthConst;
import io.github.majianzheng.jarboot.entity.User;
import io.github.majianzheng.jarboot.security.JarbootUser;
import io.github.majianzheng.jarboot.security.JwtTokenManager;
import io.github.majianzheng.jarboot.service.OpenApiService;
import io.github.majianzheng.jarboot.service.UserService;
import io.github.majianzheng.jarboot.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 鉴权接口
 * @author majianzheng
 */
@RequestMapping(value = CommonConst.AUTH_CONTEXT)
@RestController
public class AuthController {
    private static final String PARAM_USERNAME = "username";

    private static final String PARAM_PASSWORD = "password";

    @Resource
    private JwtTokenManager jwtTokenManager;
    @Resource
    private AuthenticationManager authenticationManager;
    @Resource
    private UserService userService;

    @Value("${jarboot.token.expire.seconds:7776000}")
    private long expireSeconds;

    /**
     * 获取当前登录的用户
     * @return 结果
     */
    @GetMapping(value="/getCurrentUser")
    public ResponseVo<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findUserByUsername(authentication.getName());
        return HttpResponseUtils.success(user);
    }

    /**
     * 登入系统
     * @param request http请求
     * @param response http响应
     * @return 结果
     */
    @PostMapping(value="/login")
    @EnableAuditLog("登入系统")
    public ResponseVo<JarbootUser> login(HttpServletRequest request, HttpServletResponse response) {
        String token = CommonUtils.getToken(request);
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
        String host = ClusterClientManager.getInstance().getSelfHost();
        JarbootUser jarbootUser = new JarbootUser();
        jarbootUser.setUsername(username);
        jarbootUser.setAccessToken(token);
        jarbootUser.setTokenTtl(expireSeconds);
        jarbootUser.setRoles(user.getRoles());
        jarbootUser.setAvatar(userService.getAvatar(username));
        jarbootUser.setHost(host);
        Cookie cookie = new Cookie(AuthConst.TOKEN_COOKIE_NAME, token);
        cookie.setMaxAge((int)expireSeconds);
        cookie.setPath("/");
        response.addCookie(cookie);
        if (StringUtils.isNotEmpty(ClusterClientManager.getInstance().getSelfHost())) {
            cookie = new Cookie(AuthConst.CLUSTER_COOKIE_NAME, host);
            cookie.setMaxAge((int) expireSeconds);
            cookie.setPath("/");
            response.addCookie(cookie);
        }
        return HttpResponseUtils.success(jarbootUser);
    }

    /**
     * 登出系统
     * @param request http请求
     * @param response http响应
     * @return 结果
     */
    @PostMapping(value="/logout")
    @EnableAuditLog("登出系统")
    public ResponseVo<String> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = CommonUtils.getToken(request);
        jwtTokenManager.validateToken(token);
        SecurityContextHolder.getContext().setAuthentication(null);
        Cookie cookie = new Cookie(AuthConst.TOKEN_COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        if (StringUtils.isNotEmpty(ClusterClientManager.getInstance().getSelfHost())) {
            cookie = new Cookie(AuthConst.CLUSTER_COOKIE_NAME, "");
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);
        }
        return HttpResponseUtils.success("退出成功");
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
