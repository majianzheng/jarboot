package io.github.majianzheng.jarboot.cluster;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.ServerRuntimeInfo;
import io.github.majianzheng.jarboot.event.FromOtherClusterServerMessageEvent;
import io.github.majianzheng.jarboot.common.ConcurrentWeakKeyHashMap;
import io.github.majianzheng.jarboot.common.JarbootThreadFactory;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.constant.AuthConst;
import io.github.majianzheng.jarboot.event.AbstractMessageEvent;
import io.github.majianzheng.jarboot.event.FuncReceivedEvent;
import io.github.majianzheng.jarboot.utils.CommonUtils;
import io.github.majianzheng.jarboot.utils.SettingUtils;
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

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * 集群配置
 * @author mazheng
 */
@SuppressWarnings({"squid:S2274", "PrimitiveArrayArgumentToVarargsMethod"})
public class ClusterClientManager {
    private static final Logger logger = LoggerFactory.getLogger(ClusterClientManager.class);
    private static final String NOTE_PREFIX = "#";
    private static final String CLUSTER_SECRET_KEY = "cluster-secret-key";
    private final ConcurrentWeakKeyHashMap<String, String> userTokenCache = new ConcurrentWeakKeyHashMap<>(16);
    /** 集群列表 */
    private final Map<String, ClusterClient> hosts = new LinkedHashMap<>(16);
    private final Set<String> allClusterIps = new HashSet<>(16);
    /** 集群配置是否生效 */
    private boolean enabled = false;
    /** 主节点 */
    private String masterHost;
    /** 自己 */
    private String selfHost;
    private String selfHostName;
    private byte[] clusterSecretKey = null;

    private boolean initialized = false;

    public static ClusterClientManager getInstance() {
        return ClusterConfigHolder.INST;
    }

    public Map<String, ClusterClient> getHosts() {
        return hosts;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public String getMasterHost() {
        return masterHost;
    }

    public String getSelfHost() {
        return selfHost;
    }

    public String getSelfHostName() {
        return selfHostName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ClusterClient getClient(String host) {
        return hosts.get(host);
    }

    public void notifyToOtherClusterFront(String clusterHost, AbstractMessageEvent event, String sessionId) {
        ClusterClient client = getClient(clusterHost);
        if (!enabled || null == client || !client.isOnline()) {
            return;
        }
        ClusterEventMessage req = new ClusterEventMessage();
        req.setName(ClusterEventName.NOTIFY_TO_FRONT.name());
        req.setType(ClusterEventMessage.REQ_TYPE);
        FromOtherClusterServerMessageEvent messageEvent = new FromOtherClusterServerMessageEvent();
        messageEvent.setMessage(event.message());
        messageEvent.setSessionId(sessionId);
        messageEvent.setSid(event.getSid());
        req.setBody(JsonUtils.toJsonString(messageEvent));
        client.sendMessage(req);
    }
    public void notifyToOtherClusterFront(AbstractMessageEvent event) {
        if (enabled) {
            hosts.forEach((k, client) -> {
                if (Objects.equals(selfHost, client.getHost()) || !client.isOnline()) {
                    return;
                }
                ClusterEventMessage req = new ClusterEventMessage();
                req.setName(ClusterEventName.NOTIFY_TO_FRONT.name());
                req.setType(ClusterEventMessage.REQ_TYPE);
                FromOtherClusterServerMessageEvent messageEvent = new FromOtherClusterServerMessageEvent();
                messageEvent.setMessage(event.message());
                req.setBody(JsonUtils.toJsonString(messageEvent));
                client.sendMessage(req);
            });
        }
    }

    public void execClusterFunc(FuncReceivedEvent funcEvent) {
        ClusterClient client = getClient(funcEvent.getHost());
        if (null == client) {
            return;
        }
        final int maxWait = 15000;
        funcEvent.setSessionId(String.format("%s %s",selfHost, funcEvent.getSessionId()));
        client.requestSync(ClusterEventName.EXEC_FUNC, JsonUtils.toJsonString(funcEvent), maxWait);
    }

    public boolean clusterAuth(String token, String accessClusterHost) {
        if (!enabled || Objects.equals(selfHost, accessClusterHost)) {
            return false;
        }
        ClusterClient client = getClient(accessClusterHost);
        if (null == client || !client.isOnline()) {
            return false;
        }
        final int maxWait = 15000;
        String encoded = client.requestSync(ClusterEventName.CLUSTER_AUTH, token, maxWait);
        if (StringUtils.isEmpty(encoded)) {
            logger.error("集群认证失败，host: {}, token: {}", accessClusterHost, token);
            return false;
        }
        try (ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(Base64.getDecoder().decode(encoded.getBytes(StandardCharsets.UTF_8))))) {
            Authentication authentication = (Authentication) ois.readObject();
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            return false;
        }
        return true;
    }


    public boolean authClusterToken(HttpServletRequest request) {
        if (null == clusterSecretKey) {
            // 未启用集群模式
            return false;
        }
        if (!request.getRequestURI().startsWith(CommonConst.CLUSTER_CONTEXT)) {
            // 非集群API，不支持使用cluster token认证
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
        String ip = CommonUtils.getActualIpAddr(request);
        if (!allClusterIps.contains(ip)) {
            // 非集群内部IP，禁止访问
            return false;
        }
        try {
            validAuth(token, clusterSecretKey);
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

    public void init() {
        Map<String, String> hostUuidMap = new ConcurrentHashMap<>(16);
        final ThreadPoolExecutor executorService = new ThreadPoolExecutor(
                4,
                16,
                15,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(128),
                JarbootThreadFactory.createThreadFactory("cluster-conf-init-"));
        try {
            List<String> lines = FileUtils.readLines(getClusterConfigFile(), StandardCharsets.UTF_8);
            lines.forEach(this::handleLine);
            if (null == clusterSecretKey || hosts.isEmpty()) {
                return;
            }
            logger.info("开始集群发现......");
            hosts.forEach((k, v) -> executorService.execute(() -> waitHostStarted(v, hostUuidMap)));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            executorService.shutdown();
            try {
                final int maxWait = 5;
                if (!executorService.awaitTermination(maxWait, TimeUnit.MINUTES)) {
                    logger.info("等待集群启动超时！");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            final int minCount = 2;
            if (null == clusterSecretKey || hosts.size() < minCount) {
                hosts.clear();
                selfHost = null;
                logger.info("集群模式未配置，单例模式启动");
                initialized = true;
            } else {
                final int minSecretKeyLength = 32;
                if (StringUtils.isEmpty(selfHost) || clusterSecretKey.length <= minSecretKeyLength) {
                    // 未将自己配置在配置文件中
                    final String msg = "[cluster-secret-key]的长度小于等于32或者集群配置文件中必须要包含自己，检测到未包含自己！";
                    logger.error("{}, selfHost:{}", msg, selfHost);
                    System.exit(-1);
                } else {
                    enabled = true;
                    initialized = true;
                    logger.info("集群模式启动");
                    monitorCheckHealth();
                }
            }
        }
    }

    private void monitorCheckHealth() {
        JarbootThreadFactory.createThreadFactory("cluster.health-check", true).newThread(() -> {
            for (;;) {
                try {
                    TimeUnit.SECONDS.sleep(10);
                    hosts.forEach((k, v) -> {
                        try {
                            v.health();
                        } catch (Exception e) {
                            // ignore
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    private void handleLine(String line) {
        if (StringUtils.isEmpty(line)) {
            return;
        }
        line = line.trim();
        if (line.startsWith(NOTE_PREFIX)) {
            return;
        }
        if (line.startsWith(CLUSTER_SECRET_KEY)) {
            int index = line.indexOf('=');
            if (index >= CLUSTER_SECRET_KEY.length()) {
                String temp = line.substring(index + 1).trim();
                final int minLength = 32;
                if (StringUtils.isNotEmpty(temp) && temp.length() > minLength) {
                    clusterSecretKey = temp.getBytes(StandardCharsets.UTF_8);
                }
            }
            return;
        }
        final String str = "//";
        int index = line.indexOf(str);
        if (index >= 0) {
            line = line.substring(index + str.length());
        }
        String ip = parseIp(line);
        allClusterIps.add(ip);
        ClusterClient client = new ClusterClient(line);
        hosts.put(client.getHost(), client);
    }

    private void waitHostStarted(ClusterClient client, Map<String, String> hostUuidMap) {
        final int tryCount = 60;
        try {
            for (int i = 0; i < tryCount; ++i) {
                if (checkHost(client, hostUuidMap)) {
                    break;
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean checkHost(ClusterClient client, Map<String, String> hostUuidMap) throws InterruptedException {
        try {
            logger.debug("check host {}", client.getHost());
            ServerRuntimeInfo info = client.health();
            logger.info("check cluster info host:{}, code:{}, uuid:{}", client.getHost(), info.getMachineCode(), info.getUuid());
            final StringBuilder sb = new StringBuilder();
            hostUuidMap.compute(info.getUuid(), (k, v) -> {
                if (null == v) {
                    // 无冲突
                    return client.getHost();
                }
                sb.append(v);
                return v;
            });
            boolean fatal = false;
            String conflictHost = sb.toString();
            if (StringUtils.isNotEmpty(conflictHost)) {
                // uuid冲突
                logger.error("uuid冲突！{}与{}的uuid冲突，请确保集群实例的uuid唯一性！", client.getHost(), conflictHost);
                fatal = true;
            }
            if (Objects.equals(info.getUuid(), SettingUtils.getUuid()) && Objects.equals(info.getMachineCode(), CommonUtils.getMachineCode())) {
                // 检查
                this.selfHost = client.getHost();
                this.selfHostName = client.getName();
                logger.info("自身host: {}, name: {}", selfHost, selfHostName);
            }
            if (fatal) {
                // 致命错误，程序退出
                System.exit(-1);
            }
            return true;
        } catch (Exception e) {
            TimeUnit.SECONDS.sleep(5);
        }
        return false;
    }

    public static String parseIp(String host) {
        int index = host.indexOf(':');
        String ip = index > 0 ? host.substring(0, index) : host;
        index = ip.lastIndexOf('\\');
        ip = index > 0 ? ip.substring(index + 1) : ip;
        return ip;
    }

    private static class ClusterConfigHolder {
        static final ClusterClientManager INST = new ClusterClientManager();
    }

    private void validAuth(String token, byte[] secretKeyBytes) {
        Claims claims = Jwts.parserBuilder().setSigningKey(secretKeyBytes).build()
                .parseClaimsJws(token).getBody();

        List<GrantedAuthority> authorities = AuthorityUtils
                .commaSeparatedStringToAuthorityList((String) claims.get(AuthConst.AUTHORITIES_KEY));

        User principal = new User(claims.getSubject(), StringUtils.EMPTY, authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, StringUtils.EMPTY, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
