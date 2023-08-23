package io.github.majianzheng.jarboot.security;

import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.constant.AuthConst;
import io.github.majianzheng.jarboot.dao.UserDao;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Value("${jarboot.token.expire.seconds:7776000}")
    private long expireSeconds;

    @Value("${jarboot.token.secret.key:SecretKey012345678901234567899876543210012345678901234567890123456789}")
    private String secretKey;
    @Autowired
    private UserDao userDao;
    private byte[] secretKeyBytes;

    public void init(String secretKey) {
        this.secretKey = secretKey;
        this.secretKeyBytes = null;
    }

    public byte[] getSecretKeyBytes() {
        if (secretKeyBytes == null) {
            secretKeyBytes = Decoders.BASE64.decode(secretKey);
        }
        return secretKeyBytes;
    }
    
    /**
     * Create token.
     *
     * @param username auth info
     * @return token
     */
    public String createToken(String username) {

        long now = System.currentTimeMillis();
        String roles = userDao.getUserRoles(username);
        Date validity;
        validity = new Date(now + expireSeconds * 1000L);
        
        Claims claims = Jwts.claims().setSubject(username);
        claims.put(AuthConst.AUTHORITIES_KEY, roles);
        return Jwts.builder().setClaims(claims).setExpiration(validity)
                .signWith(Keys.hmacShaKeyFor(getSecretKeyBytes()), SignatureAlgorithm.HS256).compact();
    }

    /**
     * Create openapi token.
     *
     * @param username auth info
     * @return token
     */
    public String createOpenApiToken(String username) {
        String roles = userDao.getUserRoles(username);
        Claims claims = Jwts.claims().setSubject(username);
        claims.put(AuthConst.AUTHORITIES_KEY, roles);
        return Jwts.builder().setClaims(claims)
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
                .commaSeparatedStringToAuthorityList((String) claims.get(AuthConst.AUTHORITIES_KEY));
        
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
