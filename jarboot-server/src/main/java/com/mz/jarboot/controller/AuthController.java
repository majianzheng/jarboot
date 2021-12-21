package com.mz.jarboot.controller;

import com.mz.jarboot.common.ResponseForList;
import com.mz.jarboot.common.ResponseForObject;
import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.constant.AuthConst;
import com.mz.jarboot.entity.RoleInfo;
import com.mz.jarboot.exception.AccessException;
import com.mz.jarboot.security.JarbootUser;
import com.mz.jarboot.security.JwtTokenManager;
import com.mz.jarboot.service.RoleService;
import io.jsonwebtoken.ExpiredJwtException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 鉴权接口
 * @author majianzheng
 */
@RequestMapping(value = "/api/jarboot/auth")
@Controller
public class AuthController {
    private static final int DEFAULT_PAGE_NO = 1;

    private static final String PARAM_USERNAME = "username";

    private static final String PARAM_PASSWORD = "password";

    @Autowired
    private JwtTokenManager jwtTokenManager;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private RoleService roleService;
    @Value("${jarboot.token.expire.seconds:18000}")
    private long expireSeconds;

    /**
     * 获取当前登录的用户
     * @param request Http请求
     * @return 结果
     */
    @GetMapping(value="/getCurrentUser")
    @ResponseBody
    public ResponseForObject<Object> getCurrentUser(HttpServletRequest request) {
        ResponseForObject<Object> current = new ResponseForObject<>();
        String token = resolveToken(request);
        if (StringUtils.isEmpty(token)) {
            current.setResultCode(ResultCodeConst.NOT_EXIST);
            current.setResultMsg("当前未登录");
            return current;
        }
        Authentication authentication = jwtTokenManager.getAuthentication(token);
        current.setResult(authentication.getPrincipal());
        return current;
    }

    /**
     * 登入系统
     * @param request http请求
     * @return 结果
     */
    @PostMapping(value="/login")
    @ResponseBody
    public ResponseForObject<JarbootUser> login(HttpServletRequest request) {
        String token = getToken(request);
        ResponseForObject<JarbootUser> result = new ResponseForObject<>();

        String username = request.getParameter(PARAM_USERNAME);
        if (StringUtils.isEmpty(username) && StringUtils.isNotBlank(token)) {
            // 已经登录了，鉴定权限
            try {
                jwtTokenManager.validateToken(token);
            } catch (ExpiredJwtException e) {
                throw new AccessException("Token expired!");
            } catch (Exception e) {
                throw new AccessException("Token invalid!");
            }
            Authentication authentication = jwtTokenManager.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            username = authentication.getName();
        } else {
            try {
                String password = request.getParameter(PARAM_PASSWORD);
                token = resolveTokenFromUser(username, password);
            } catch (Exception e) {
                result.setResultCode(HttpStatus.UNAUTHORIZED.value());
                result.setResultMsg("Login failed, user name or password error.");
            }
        }
        JarbootUser user = new JarbootUser();
        user.setUsername(username);
        user.setAccessToken(token);
        user.setTokenTtl(expireSeconds);
        List<RoleInfo> roleInfoInfoList = getRoles(username);
        if (!CollectionUtils.isEmpty(roleInfoInfoList)) {
            for (RoleInfo roleInfo : roleInfoInfoList) {
                if (roleInfo.getRole().equals(AuthConst.ADMIN_ROLE)) {
                    user.setGlobalAdmin(true);
                    break;
                }
            }
        }
        result.setResult(user);

        return result;
    }

    /**
     * 根据用户名获取角色
     * @param username 用户名
     * @return 角色
     */
    public List<RoleInfo> getRoles(String username) {
        ResponseForList<RoleInfo> roleInfoList = roleService.getRolesByUserName(username, DEFAULT_PAGE_NO, Integer.MAX_VALUE);
        List<RoleInfo> roleInfos = roleInfoList.getResult();
        if (CollectionUtils.isEmpty(roleInfos) && AuthConst.JARBOOT_USER.equalsIgnoreCase(username)) {
            roleInfos = new ArrayList<>();
            RoleInfo roleInfo = new RoleInfo();
            roleInfo.setRole(AuthConst.ADMIN_ROLE);
            roleInfo.setUsername(AuthConst.JARBOOT_USER);
            roleInfos.add(roleInfo);
        }
        return roleInfos;
    }

    private String getToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AuthConst.AUTHORIZATION_HEADER);
        if (StringUtils.isNotBlank(bearerToken) && bearerToken.startsWith(AuthConst.TOKEN_PREFIX)) {
            return bearerToken.substring(7);
        }
        return request.getParameter(AuthConst.ACCESS_TOKEN);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = getToken(request);
        if (StringUtils.isBlank(bearerToken)) {
            String userName = request.getParameter(PARAM_USERNAME);
            String password = request.getParameter(PARAM_PASSWORD);
            bearerToken = resolveTokenFromUser(userName, password);
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
            throw new AccessException("Login failed");
        }

        if (null == authenticate || StringUtils.isBlank(authenticate.getName())) {
            finalName = userName;
        } else {
            finalName = authenticate.getName();
        }

        return jwtTokenManager.createToken(finalName);
    }
}
