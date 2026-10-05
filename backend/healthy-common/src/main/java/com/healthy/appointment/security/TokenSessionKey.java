package com.healthy.appointment.security;

import com.healthy.appointment.constant.Constant;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Builds the Redis key for a token session without storing the raw token. */
public final class TokenSessionKey {
    private TokenSessionKey() {
    }

    public static String redisKey(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return Constant.AUTH_TOKEN_PREFIX + builder;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }
}
