package io.github.majianzheng.jarboot.constant;

/**
 * @author majianzheng
 */
public class AuthConst {
    private AuthConst(){}

    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String ACCESS_TOKEN = "accessToken";
    public static final String CLUSTER_TOKEN = "clusterToken";

    public static final String TOKEN_PREFIX = "Bearer ";

    public static final String JARBOOT_USER = "jarboot";
    public static final String ADMIN_ROLE = "ROLE_ADMIN";

    public static final String SYS_ROLE = "ROLE_SYS";
    public static final String CLUSTER_ROLE = "ROLE_CLUSTER";

    public static final String ROLE_PREFIX = "ROLE_";

    public static final long MAX_ROLE = 10000;

    public static final String AUTHORITIES_KEY = "auth";

    public static final String ACCESS_CLUSTER_HOST = "Access-Cluster-Host";
}
