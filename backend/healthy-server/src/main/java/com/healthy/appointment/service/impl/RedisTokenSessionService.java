package com.healthy.appointment.service.impl;

import com.healthy.appointment.constant.Constant;
import com.healthy.appointment.service.TokenSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisTokenSessionService implements TokenSessionService {
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(String token, Long userId, Duration ttl) {
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        stringRedisTemplate.opsForValue().set(tokenKey(token), String.valueOf(userId), ttl);
    }

    @Override
    public boolean isActive(String token, Long userId) {
        String storedUserId = stringRedisTemplate.opsForValue().get(tokenKey(token));
        return String.valueOf(userId).equals(storedUserId);
    }

    @Override
    public void remove(String token) {
        stringRedisTemplate.delete(tokenKey(token));
    }

    private String tokenKey(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return Constant.AUTH_TOKEN_PREFIX + toHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
