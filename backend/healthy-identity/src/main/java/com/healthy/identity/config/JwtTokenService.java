package com.healthy.identity.config;

import com.healthy.appointment.security.JwtTokenSupport;
import com.healthy.identity.entity.SysUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenService {
    private final JwtProperties jwtProperties;

    public String createToken(SysUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getTtl());
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole())
                .claim("username", user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(JwtTokenSupport.signingKey(jwtProperties.getSecret()))
                .compact();
    }

    public Claims parseClaims(String token) {
        return JwtTokenSupport.parseClaims(token, jwtProperties.getSecret());
    }

    public Duration remainingTtl(String token) {
        Instant expiresAt = parseClaims(token).getExpiration().toInstant();
        return Duration.between(Instant.now(), expiresAt);
    }
}
