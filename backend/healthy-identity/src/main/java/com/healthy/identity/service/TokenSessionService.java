package com.healthy.identity.service;

import java.time.Duration;

public interface TokenSessionService {
    void save(String token, Long userId, Duration ttl);

    void remove(String token);
}
