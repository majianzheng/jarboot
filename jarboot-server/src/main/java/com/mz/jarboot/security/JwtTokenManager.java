package com.mz.jarboot.security;

import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.constant.AuthConst;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * @author majianzheng
 */
@Component
public class JwtTokenManager {
    
    private static final String AUTHORITIES_KEY = "auth";

    @Value("${jarboot.token.expire.seconds:18000}")
    private long expireSeconds;

    @Value("${jarboot.token.secret.key:}")
    private String secretKey;
    private byte[] secretKeyBytes;

    public byte[] getSecretKeyBytes() {
        if (secretKeyBytes == null) {
            secretKeyBytes = Decoders.BASE64.decode(secretKey);
        }
        return secretKeyBytes;
    }
    
    /**
     * Create token.
     *
     * @param authentication auth info
     * @return token
     */
    public String createToken(Authentication authentication) {
        return createToken(authentication.getName());
    }
    
    /**
     * Create token.
     *
     * @param userName auth info
     * @return token
     */
    public String createToken(String userName) {
        
        long now = System.currentTimeMillis();
        
        Date validity;
        validity = new Date(now + expireSeconds * 1000L);
        
        Claims claims = Jwts.claims().setSubject(userName);
        return Jwts.builder().setClaims(claims).setExpiration(validity)
                .signWith(Keys.hmacShaKeyFor(getSecretKeyBytes()), SignatureAlgorithm.HS256).compact();
    }
    
    /**
     * Get auth Info.
     *
     * @param token token
     * @return auth info
     */
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(getSecretKeyBytes()).build()
                .parseClaimsJws(token).getBody();
        
        List<GrantedAuthority> authorities = AuthorityUtils
                .commaSeparatedStringToAuthorityList((String) claims.get(AUTHORITIES_KEY));
        
        User principal = new User(claims.getSubject(), StringUtils.EMPTY, authorities);
        return new UsernamePasswordAuthenticationToken(principal, StringUtils.EMPTY, authorities);
    }
    
    /**
     * validate token.
     *
     * @param token token
     */
    public void validateToken(String token) {
        if (!StringUtils.isBlank(token) && token.startsWith(AuthConst.TOKEN_PREFIX)) {
            token = token.substring(AuthConst.TOKEN_PREFIX.length());
        }
        Jwts.parserBuilder().setSigningKey(getSecretKeyBytes()).build().parseClaimsJws(token);
    }
    
}
