package com.mz.jarboot.security;

import com.mz.jarboot.common.pojo.ResponseSimple;
import com.mz.jarboot.common.utils.HttpResponseUtils;
import com.mz.jarboot.common.utils.JsonUtils;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.constant.AuthConst;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author majianzheng
 */
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    private final JwtTokenManager tokenManager;

    public JwtAuthenticationTokenFilter(JwtTokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String jwt = resolveToken(request);
        
        if (!StringUtils.isBlank(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                this.tokenManager.validateToken(jwt);
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
                handleError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "token校验失败，请重新登录");
                return;
            }
            Authentication authentication = this.tokenManager.getAuthentication(jwt);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } else {
            handleError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "未登录");
        }
    }

    private void handleError(HttpServletResponse response, int status, int code, String msg) throws IOException {
        ResponseSimple responseVo = HttpResponseUtils.error(code, msg);
        response.setStatus(status);
        response.setContentType("application/json");
        response.getOutputStream().write(JsonUtils.toJsonBytes(responseVo));
        response.getOutputStream().flush();
    }

    /**
     * Get token from header.
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AuthConst.AUTHORIZATION_HEADER);
        if (!StringUtils.isBlank(bearerToken)) {
            if (bearerToken.startsWith(AuthConst.TOKEN_PREFIX)) {
                return bearerToken.substring(AuthConst.TOKEN_PREFIX.length());
            }
            return bearerToken;
        }
        String jwt = request.getParameter(AuthConst.ACCESS_TOKEN);
        if (!StringUtils.isBlank(jwt)) {
            return jwt;
        }
        return null;
    }
}
