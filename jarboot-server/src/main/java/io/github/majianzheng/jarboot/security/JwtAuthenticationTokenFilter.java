package io.github.majianzheng.jarboot.security;

import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.common.pojo.ResponseSimple;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.constant.AuthConst;
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
                String accessClusterHost = getAccessClusterHost(request);
                if (ClusterClientManager.getInstance().clusterAuth(jwt, accessClusterHost)) {
                    chain.doFilter(request, response);
                    return;
                }
                Authentication authentication = this.tokenManager.getAuthentication(jwt);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
                handleError(response, HttpServletResponse.SC_UNAUTHORIZED, HttpServletResponse.SC_UNAUTHORIZED, "token校验失败，请重新登录");
                return;
            }
            chain.doFilter(request, response);
        } else {
            if (ClusterClientManager.getInstance().authClusterToken(request)) {
                chain.doFilter(request, response);
                return;
            }
            handleError(response, HttpServletResponse.SC_UNAUTHORIZED, HttpServletResponse.SC_UNAUTHORIZED, "未登录");
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

    private String getAccessClusterHost(HttpServletRequest request) {
        final String clusterHostKey = "Access-Cluster-Host";
        String accessClusterHost = request.getHeader(clusterHostKey);
        if (StringUtils.isEmpty(accessClusterHost)) {
            accessClusterHost = request.getParameter(clusterHostKey);
        }
        return accessClusterHost;
    }
}
