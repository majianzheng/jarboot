package io.github.majianzheng.jarboot.utils;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.common.utils.NetworkUtils;
import io.github.majianzheng.jarboot.common.utils.OSUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.constant.AuthConst;
import io.jsonwebtoken.lang.Collections;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.CollectionUtils;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 工具类
 * @author mazheng
 */
@Slf4j
public class CommonUtils {
    /**
     * 获取用户真实IP地址，不使用request.getRemoteAddr();的原因是有可能用户使用了代理软件方式避免真实IP地址,
     * <p>
     * 可是，如果通过了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP值，究竟哪个才是真正的用户端的真实IP呢？
     * 答案是取X-Forwarded-For中第一个非unknown的有效IP字符串。
     * <p>
     * 如：X-Forwarded-For：192.168.1.110, 192.168.1.120, 192.168.1.130,
     * 192.168.1.100
     * <p>
     * 用户真实IP为： 192.168.1.110
     *
     * @param request 请求
     * @return 真实客户端IP
     */
    public static String getActualIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        final String unknown = "unknown";
        if (ip == null || ip.isEmpty() || unknown.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || unknown.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || unknown.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || unknown.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || unknown.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip.contains(CommonConst.COMMA_SPLIT)) {
            return ip.split(CommonConst.COMMA_SPLIT)[0];
        }
        return ip;
    }

    public static boolean needProxy(String clusterHost) {
        if (!ClusterClientManager.getInstance().isEnabled() || StringUtils.isEmpty(clusterHost)) {
            return false;
        }
        if (Objects.equals(SettingUtils.getLocalhost(), clusterHost)) {
            return false;
        }
        return !Objects.equals(ClusterClientManager.getInstance().getSelfHost(), clusterHost);
    }

    public static String getSessionClusterHost(Session session) {
        return getSessionParam("clusterHost", session);
    }

    public static String getSessionParam(String key, Session session) {
        List<String> param = session.getRequestParameterMap().get(key);
        if (Collections.isEmpty(param)) {
            return StringUtils.EMPTY;
        }
        return param.get(0);
    }

    public static void setDownloadHeader(HttpServletResponse response, String filename) {
        if (StringUtils.isNotEmpty(filename)) {
            try {
                //支持中文、空格
                filename = new String(filename.getBytes("gb2312"), StandardCharsets.ISO_8859_1);
            } catch (Exception e) {
                //ignore
            }
            final String contentDisposition = String.format("attachment; filename=\"%s\"", filename);
            response.setHeader("Content-Disposition", contentDisposition);
        }
        response.setHeader("content-type", "file");
        response.setContentType("application/octet-stream");
    }

    public static String createJvmSid(String pid) {
        return String.format("jvm-%08x%08x", SettingUtils.getUuid().hashCode(), pid.hashCode());
    }

    public static String getMachineCode() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hal = systemInfo.getHardware();
        final String unknown = "unknown";
        String code = hal.getComputerSystem().getSerialNumber();
        if (unknown.equals(code)) {
            code = getFromUuidFile();
        }
        return code;
    }

    private static String getFromUuidFile() {
        List<String> addrList = NetworkUtils.getMacAddrList();
        boolean useRandomUuid = useRandomUuid(addrList);
        File uuidFile = FileUtils.getFile(SettingUtils.getHomePath(), "data", ".uuid");
        String code = StringUtils.EMPTY;
        String content = StringUtils.EMPTY;
        if (uuidFile.exists()) {
            try {
                content = FileUtils.readFileToString(uuidFile, StandardCharsets.UTF_8);
            } catch (Exception e) {
                // ignore
            }
        }
        int index = content.indexOf('-');
        if (index > 0) {
            code = content.substring(0, index);
            if (useRandomUuid) {
                return code;
            }
            for (String addr : addrList) {
                String hash = String.format("%08x", addr.hashCode());
                if (code.contains(hash)) {
                    break;
                }
            }
        }

        if (StringUtils.isEmpty(code)) {
            if (useRandomUuid) {
                code = String.format("%08x", UUID.randomUUID().hashCode());
            } else {
                code = genMachineCodeByMacAddr(addrList);
            }
        }
        return code;
    }

    private static boolean useRandomUuid(List<String> addrList) {
        boolean isDocker = (Boolean.getBoolean(CommonConst.DOCKER) && StringUtils.isNotEmpty(System.getenv("HOSTNAME")));
        if (isDocker) {
            return true;
        }
        return CollectionUtils.isEmpty(addrList);
    }

    private static String genMachineCodeByMacAddr(List<String> addrList) {
        addrList.sort(String::compareTo);
        String code1 = addrList.get(0);
        String code2 = addrList.get(addrList.size() - 1);
        final int two = 2;
        if (addrList.size() > two) {
            String code3 = addrList.get(addrList.size() / two);
            return String.format("%08x%08x%08x", code1.hashCode(), code2.hashCode(), code3.hashCode());
        }
        return String.format("%08x%08x", code1.hashCode(), code2.hashCode());
    }

    public static String getToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AuthConst.TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (token.startsWith(AuthConst.TOKEN_PREFIX)) {
                        token = token.substring(AuthConst.TOKEN_PREFIX.length());
                    }
                    return token;
                }
            }
        }
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

    public static String getAccessClusterHost(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AuthConst.CLUSTER_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        String accessClusterHost = request.getHeader(AuthConst.ACCESS_CLUSTER_HOST);
        if (StringUtils.isEmpty(accessClusterHost)) {
            accessClusterHost = request.getParameter(AuthConst.ACCESS_CLUSTER_HOST);
        }
        return accessClusterHost;
    }

    /**
     * 获取当前登录的用户名
     * @return 用户名
     */
    public static String getLoginUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (null == authentication) {
            return StringUtils.EMPTY;
        }
        return authentication.getName();
    }

    /**
     * 获取当前登录的角色
     * @return 角色列表
     */
    public static Set<String> getLoginRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (null == authentication) {
            return new HashSet<>();
        }
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    public static String getHomeEnv() {
        return OSUtils.isWindows() ? "%JARBOOT_HOME%" : "$JARBOOT_HOME";
    }

    private CommonUtils() {}
}
