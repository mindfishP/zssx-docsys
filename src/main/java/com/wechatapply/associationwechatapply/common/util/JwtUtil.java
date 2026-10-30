package com.wechatapply.associationwechatapply.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

/**
 * JWT 工具：签发与解析登录令牌（SRS 7.1 接口安全约定）。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMinutes;

    public JwtUtil(@Value("${zssx.jwt.secret}") String secret,
                   @Value("${zssx.jwt.expire-minutes:720}") long expireMinutes) {

        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        //this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMinutes = expireMinutes;
    }


    public String issue(Long userId, String username, String roleCode, String assocCode) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", roleCode)
                .claim("assoc", assocCode == null ? "" : assocCode)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMinutes * 60_000L))
                .signWith(key)
                .compact();
    }

    /** 解析失败（过期/伪造）抛出 JwtException，由拦截器统一处理为 401。 */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }
}
