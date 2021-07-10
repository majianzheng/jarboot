package com.mz.jarboot.constant;

public class AuthConst {
    private AuthConst(){}

    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String ACCESS_TOKEN = "accessToken";

    public static final String SECURITY_IGNORE_URLS_SPILT_CHAR = ",";

    public static final String LOGIN_ENTRY_POINT = "/api/auth/login";

    public static final String TOKEN_BASED_AUTH_ENTRY_POINT = "/api/auth/**";

    public static final String TOKEN_PREFIX = "Bearer ";

    public static final String JARBOOT_USER = "jarboot";
    public static final String ADMIN_ROLE = "ROLE_ADMIN";

    public static final String REQUEST_PATH_SEPARATOR = "-->";

    public static final long MAX_ROLE = 10000;
}
