package com.healthy.gateway.filter;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.result.ApiResponse;
import com.healthy.appointment.security.AuthenticatedUserHeaders;
import com.healthy.appointment.security.JwtTokenSupport;
import com.healthy.appointment.security.TokenSessionKey;
import com.healthy.gateway.config.GatewayAuthProperties;
import com.healthy.gateway.config.GatewayJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class GatewayAuthenticationFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(GatewayAuthenticationFilter.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final GatewayAuthProperties authProperties;
    private final GatewayJwtProperties jwtProperties;
    private final ObjectMapper objectMapper;

    public GatewayAuthenticationFilter(
            ReactiveStringRedisTemplate redisTemplate,
            GatewayAuthProperties authProperties,
            GatewayJwtProperties jwtProperties,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.authProperties = authProperties;
        this.jwtProperties = jwtProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange sanitizedExchange = sanitizeIdentityHeaders(exchange);
        if (!authProperties.isEnabled() || isPublicRequest(sanitizedExchange.getRequest())) {
            return chain.filter(sanitizedExchange);
        }

        AuthenticatedUser user;
        String token;
        try {
            token = extractBearerToken(sanitizedExchange.getRequest());
            user = parseUser(token);
        } catch (JwtException | IllegalArgumentException exception) {
            return writeError(sanitizedExchange, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
        }

        return redisTemplate.opsForValue().get(TokenSessionKey.redisKey(token))
                .map(storedUserId -> String.valueOf(user.id()).equals(storedUserId)
                        ? SessionState.ACTIVE : SessionState.INACTIVE)
                .defaultIfEmpty(SessionState.INACTIVE)
                .doOnError(exception -> log.error("Gateway token-session lookup failed", exception))
                .onErrorReturn(SessionState.UNAVAILABLE)
                .flatMap(sessionState -> switch (sessionState) {
                    case ACTIVE -> chain.filter(withAuthenticatedUser(sanitizedExchange, user));
                    case INACTIVE -> writeError(sanitizedExchange, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
                    case UNAVAILABLE -> writeError(
                            sanitizedExchange, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private ServerWebExchange sanitizeIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(AuthenticatedUserHeaders.USER_ID);
                    headers.remove(AuthenticatedUserHeaders.ROLE);
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    private ServerWebExchange withAuthenticatedUser(ServerWebExchange exchange, AuthenticatedUser user) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.set(AuthenticatedUserHeaders.USER_ID, String.valueOf(user.id()));
                    headers.set(AuthenticatedUserHeaders.ROLE, user.role());
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    private boolean isPublicRequest(ServerHttpRequest request) {
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return true;
        }
        String path = request.getPath().pathWithinApplication().value();
        return "/api/auth/login".equals(path)
                || "/api/auth/register".equals(path)
                || "/api/health".equals(path)
                || "/error".equals(path)
                || "/doctor-service/actuator/health".equals(path)
                || path.equals("/actuator")
                || path.startsWith("/actuator/");
    }

    private String extractBearerToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header is missing or invalid");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new IllegalArgumentException("Bearer token is blank");
        }
        return token;
    }

    private AuthenticatedUser parseUser(String token) {
        Claims claims = JwtTokenSupport.parseClaims(token, jwtProperties.getSecret());
        Long userId = Long.valueOf(claims.getSubject());
        String role = claims.get("role", String.class);
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("JWT role claim is missing");
        }
        return new AuthenticatedUser(userId, role);
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, ErrorCode errorCode) {
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(ApiResponse.failure(errorCode));
        } catch (JacksonException exception) {
            return Mono.error(exception);
        }
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private enum SessionState {
        ACTIVE,
        INACTIVE,
        UNAVAILABLE
    }

    private record AuthenticatedUser(Long id, String role) {
    }
}
