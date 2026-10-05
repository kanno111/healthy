package com.healthy.appointment.config;

import com.healthy.appointment.entity.SysUser;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {
    @Test
    void createsTokenThatCanBeParsedBySharedJwtSupport() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("healthy-appointment-test-secret-key-must-be-at-least-32-bytes");
        properties.setTtl(Duration.ofHours(1));
        JwtTokenService jwtTokenService = new JwtTokenService(properties);

        SysUser user = new SysUser();
        user.setId(8L);
        user.setRole("STAFF");
        user.setUsername("staff_demo");

        String token = jwtTokenService.createToken(user);
        var claims = jwtTokenService.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("8");
        assertThat(claims.get("role", String.class)).isEqualTo("STAFF");
        assertThat(claims.get("username", String.class)).isEqualTo("staff_demo");
    }
}
