package com.mz.jarboot.constant;

/**
 * @author majianzheng
 */
public class AuthConst {
    private AuthConst(){}

    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String ACCESS_TOKEN = "accessToken";

    public static final String TOKEN_PREFIX = "Bearer ";

    public static final String JARBOOT_USER = "jarboot";
    public static final String ADMIN_ROLE = "ROLE_ADMIN";

    public static final String ROLE_PREFIX = "ROLE_";

    public static final long MAX_ROLE = 10000;
}
