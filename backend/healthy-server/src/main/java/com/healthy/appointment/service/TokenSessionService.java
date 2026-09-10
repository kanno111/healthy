package com.healthy.appointment.service;

import java.time.Duration;

/**
 * 维护当前有效的登录 Token。Redis 中存在记录，才代表该 JWT 会话仍然有效。
 */
public interface TokenSessionService {
    void save(String token, Long userId, Duration ttl);

    boolean isActive(String token, Long userId);

    void remove(String token);
}
