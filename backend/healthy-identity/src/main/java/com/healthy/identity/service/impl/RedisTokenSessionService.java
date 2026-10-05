package com.healthy.identity.service.impl;

import com.healthy.appointment.security.TokenSessionKey;
import com.healthy.identity.service.TokenSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
        stringRedisTemplate.opsForValue()
                .set(TokenSessionKey.redisKey(token), String.valueOf(userId), ttl);
    }

    @Override
    public void remove(String token) {
        stringRedisTemplate.delete(TokenSessionKey.redisKey(token));
    }
}
