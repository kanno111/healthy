package com.healthy.gateway.filter;

import com.healthy.appointment.security.AuthenticatedUserHeaders;
import com.healthy.appointment.security.JwtTokenSupport;
import com.healthy.gateway.config.GatewayAuthProperties;
import com.healthy.gateway.config.GatewayJwtProperties;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayAuthenticationFilterTest {
    private static final String SECRET = "healthy-appointment-test-secret-key-must-be-at-least-32-bytes";

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    private GatewayAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        GatewayAuthProperties authProperties = new GatewayAuthProperties();
        GatewayJwtProperties jwtProperties = new GatewayJwtProperties();
        jwtProperties.setSecret(SECRET);
        filter = new GatewayAuthenticationFilter(
                redisTemplate, authProperties, jwtProperties, JsonMapper.builder().build());
    }

    @Test
    void replacesClientSuppliedIdentityHeadersAfterValidatingSession() {
        String token = tokenFor(8L, "STAFF");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.just("8"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/api/admin/doctors")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(AuthenticatedUserHeaders.USER_ID, "999")
                .header(AuthenticatedUserHeaders.ROLE, "PATIENT")
                .build());

        filter.filter(exchange, capture(forwarded)).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst(AuthenticatedUserHeaders.USER_ID))
                .isEqualTo("8");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst(AuthenticatedUserHeaders.ROLE))
                .isEqualTo("STAFF");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer " + token);
    }

    @Test
    void rejectsRequestWhenTokenSessionIsMissing() {
        String token = tokenFor(8L, "STAFF");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/api/admin/doctors")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build());

        filter.filter(exchange, unexpectedChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsMissingAuthorizationWithoutConsultingRedis() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/api/admin/doctors")
                .build());

        filter.filter(exchange, unexpectedChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void allowsLoginWithoutTokenButStillStripsSpoofedIdentityHeaders() {
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .post("/api/auth/login")
                .header(AuthenticatedUserHeaders.USER_ID, "999")
                .header(AuthenticatedUserHeaders.ROLE, "STAFF")
                .build());

        filter.filter(exchange, capture(forwarded)).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst(AuthenticatedUserHeaders.USER_ID)).isNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst(AuthenticatedUserHeaders.ROLE)).isNull();
        verify(redisTemplate, never()).opsForValue();
    }

    private GatewayFilterChain capture(AtomicReference<ServerWebExchange> forwarded) {
        return exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        };
    }

    private GatewayFilterChain unexpectedChain() {
        return exchange -> Mono.error(new AssertionError("Request should not reach the downstream service"));
    }

    private String tokenFor(Long userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plus(1, ChronoUnit.HOURS)))
                .signWith(JwtTokenSupport.signingKey(SECRET))
                .compact();
    }
}
