package com.mz.jarboot.security;

public class JarbootUser {
    private String username;
    private String accessToken;
    private long tokenTtl;
    private boolean globalAdmin;

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

    public boolean getGlobalAdmin() {
        return globalAdmin;
    }

    public void setGlobalAdmin(boolean globalAdmin) {
        this.globalAdmin = globalAdmin;
    }
}
