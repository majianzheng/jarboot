package com.mz.jarboot.cluster;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.pojo.ServerRuntimeInfo;
import com.mz.jarboot.common.AnsiLog;
import com.mz.jarboot.common.ConcurrentWeakKeyHashMap;
import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.common.JarbootThreadFactory;
import com.mz.jarboot.common.utils.HttpUtils;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.constant.AuthConst;
import com.mz.jarboot.utils.CommonUtils;
import com.mz.jarboot.utils.SettingUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 集群配置
 * @author mazheng
 */
@SuppressWarnings({"squid:S2274", "PrimitiveArrayArgumentToVarargsMethod"})
public class ClusterConfig {
    private static final Logger logger = LoggerFactory.getLogger(ClusterConfig.class);
    private static final String NOTE_PREFIX = "#";
    private static final String CLUSTER_SECRET_KEY = "cluster-secret-key";
    private final ConcurrentWeakKeyHashMap<String, String> userTokenCache = new ConcurrentWeakKeyHashMap<>(16);
    /** 集群列表 */
    private final Set<String> hosts = new HashSet<>(16);
    private final Set<String> allClusterIps = new HashSet<>(16);
    /** 主节点 */
    private String masterHost;
    /** 自己 */
    private String selfHost;
    private byte[] clusterSecretKey = null;

    public static ClusterConfig getInstance() {
        return ClusterConfigHolder.INST;
    }

    public Set<String> getHosts() {
        return hosts;
    }

    public String getMasterHost() {
        return masterHost;
    }

    public String getSelfHost() {
        return selfHost;
    }

    public boolean authClusterToken(HttpServletRequest request) {
        if (!request.getRequestURI().contains(CommonConst.CLUSTER_CONTEXT)) {
            // 非集群API，不支持使用cluster token认证
            return false;
        }
        String ip = CommonUtils.getActualIpAddr(request);
        if (!allClusterIps.contains(ip)) {
            // 非集群内部IP，禁止访问
            return false;
        }
        String token = request.getHeader(AuthConst.CLUSTER_TOKEN);
        if (StringUtils.isEmpty(token)) {
            token = request.getParameter(AuthConst.CLUSTER_TOKEN);
        }
        if (StringUtils.isEmpty(token)) {
            // 无集群专用token
            return false;
        }
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(clusterSecretKey).build()
                    .parseClaimsJws(token).getBody();

            List<GrantedAuthority> authorities = AuthorityUtils
                    .commaSeparatedStringToAuthorityList((String) claims.get(AuthConst.AUTHORITIES_KEY));

            User principal = new User(claims.getSubject(), StringUtils.EMPTY, authorities);
            Authentication authentication = new UsernamePasswordAuthenticationToken(principal, StringUtils.EMPTY, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public String createClusterToken(String username) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put(AuthConst.AUTHORITIES_KEY, AuthConst.CLUSTER_ROLE);
        return Jwts.builder().setClaims(claims)
                .signWith(Keys.hmacShaKeyFor(clusterSecretKey), SignatureAlgorithm.HS256).compact();
    }

    public String getClusterToken(String username) {
        return userTokenCache.computeIfAbsent(username, k -> createClusterToken(username));
    }

    private File getClusterConfigFile() {
        return FileUtils.getFile(SettingUtils.getHomePath(), "conf", "cluster.conf");
    }

    private ServerRuntimeInfo getServerInfo(String host) {
        String url;
        final String http = "http";
        if (host.startsWith(http)) {
            url = host + CommonConst.SERVER_RUNTIME_CONTEXT;
        } else {
            url = String.format("%s://%s%s", http, host, CommonConst.SERVER_RUNTIME_CONTEXT);
        }
        return HttpUtils.getObj(url, ServerRuntimeInfo.class, null);
    }

    public void init() {
        ExecutorService executorService = new ThreadPoolExecutor(
                4,
                16,
                15,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(128),
                JarbootThreadFactory.createThreadFactory("cluster-conf-init-"));
        try {
            List<String> lines = FileUtils.readLines(getClusterConfigFile(), StandardCharsets.UTF_8);
            lines = lines
                    .stream()
                    .map(String::trim)
                    .filter(this::filterLine)
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(lines)) {
                return;
            }
            lines.forEach(line -> executorService.execute(() -> waitHostStarted(line)));
        } catch (Exception e) {
            // ignore
        } finally {
            try {
                if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
                    logger.warn("Wait cluster host start timeout.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executorService.shutdown();
            if (null == clusterSecretKey || hosts.isEmpty()) {
                logger.info("集群模式未配置，单例模式启动");
            }
        }
    }

    private boolean filterLine(String line) {
        if (line.isEmpty() || line.startsWith(NOTE_PREFIX)) {
            return false;
        }
        if (line.startsWith(CLUSTER_SECRET_KEY)) {
            int index = line.indexOf('=');
            if (index >= CLUSTER_SECRET_KEY.length()) {
                String temp = line.substring(index + 1).trim();
                if (StringUtils.isNotEmpty(temp)) {
                    clusterSecretKey = temp.getBytes(StandardCharsets.UTF_8);
                }
            }
            return false;
        }
        return true;
    }

    private void waitHostStarted(String lineHost) {
        for (int i = 0; i < 60; ++i) {
            try {
                ServerRuntimeInfo info = getServerInfo(lineHost);
                if (Objects.equals(info.getUuid(), SettingUtils.getUuid())) {
                    // 自己
                    this.selfHost = lineHost;
                } else {
                    hosts.add(lineHost);
                }
                int index = lineHost.indexOf(':');
                String ip = index > 0 ? lineHost.substring(0, index) : lineHost;
                allClusterIps.add(ip);
                break;
            } catch (JarbootException e) {
                if (401 == e.getErrorCode()) {
                    logger.error("认证失败，{}与当前服务的cluster-secret-key不一致", lineHost);
                    break;
                }
                // ignore
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static class ClusterConfigHolder {
        static final ClusterConfig INST = new ClusterConfig();
    }
}
