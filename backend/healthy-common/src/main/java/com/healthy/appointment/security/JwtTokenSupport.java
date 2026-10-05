package com.healthy.appointment.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/** Shared JWT signature and claim parsing support for resource services. */
public final class JwtTokenSupport {
    private JwtTokenSupport() {
    }

    public static Claims parseClaims(String token, String secret) {
        return Jwts.parser()
                .verifyWith(signingKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static SecretKey signingKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must not be blank");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
