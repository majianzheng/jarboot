package io.github.majianzheng.jarboot.security;

/**
 * @author majianzheng
 */
public class JarbootUser {
    private String username;
    private String accessToken;

    private String avatar;
    private long tokenTtl;
    private String roles;
    private String host;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public long getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(long tokenTtl) {
        this.tokenTtl = tokenTtl;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
