package com.healthy.appointment.config;

import com.healthy.appointment.entity.SysUser;
import com.healthy.appointment.security.JwtTokenSupport;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.Duration;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenService {
    private final JwtProperties jwtProperties;

    //
    public String createToken(SysUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getTtl());
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole())
                .claim("username", user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey())
                .compact();
    }

    /**
     * 校验 JWT 的签名与有效期，并返回其中保存的声明。
     * 无效或已过期的令牌会由 JJWT 抛出异常，调用方统一转换为未登录错误。
     */
    public Claims parseClaims(String token) {
        return JwtTokenSupport.parseClaims(token, jwtProperties.getSecret());
    }

    public Duration remainingTtl(String token) {
        Instant expiresAt = parseClaims(token).getExpiration().toInstant();
        return Duration.between(Instant.now(), expiresAt);
    }


    //得到secretKey  用于
    private SecretKey signingKey() {
        return JwtTokenSupport.signingKey(jwtProperties.getSecret());
    }
}
