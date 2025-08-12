package io.github.majianzheng.jarboot.security;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.common.pojo.ResponseSimple;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.utils.CommonUtils;
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
        String jwt = CommonUtils.getToken(request);
        
        if (!StringUtils.isBlank(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String accessClusterHost = CommonUtils.getAccessClusterHost(request);
                if (ClusterClientManager.getInstance().clusterAuth(jwt, accessClusterHost)) {
                    chain.doFilter(request, response);
                    return;
                }
                Authentication authentication = this.tokenManager.getAuthentication(jwt);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
                handleError(response, "token校验失败，请重新登录");
                return;
            }
            chain.doFilter(request, response);
        } else {
            if (request.getRequestURI().startsWith(CommonConst.CLUSTER_CONTEXT) && ClusterClientManager.getInstance().authClusterToken(request)) {
                chain.doFilter(request, response);
                return;
            }
            if (!"/".equals(request.getRequestURI())) {
                logger.info(String.format("当前未登录，方法：%s，url: %s，query: %s，remoteIp：%s",
                        request.getMethod(), request.getRequestURL(), request.getQueryString(), request.getRemoteAddr()));
            }
            handleError(response, "未登录");
        }
    }

    private void handleError(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getOutputStream().write(JsonUtils.toJsonBytes(new ResponseSimple(HttpServletResponse.SC_UNAUTHORIZED, msg)));
        response.getOutputStream().flush();
    }
}
