package com.healthy.appointment.interceptor;

import com.healthy.appointment.config.JwtTokenService;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.service.TokenSessionService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {
    public static final String CURRENT_USER_ID = "currentUserId";
    public static final String CURRENT_USER_ROLE = "currentUserRole";

    private final JwtTokenService jwtTokenService;
    private final TokenSessionService tokenSessionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = extractBearerToken(request.getHeader("Authorization"));
        try {
            Claims claims = jwtTokenService.parseClaims(token);
            Long userId = Long.valueOf(claims.getSubject());
            if (!tokenSessionService.isActive(token, userId)) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            request.setAttribute(CURRENT_USER_ID, userId);
            request.setAttribute(CURRENT_USER_ROLE, claims.get("role", String.class));
            return true;
        } catch (BusinessException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    public String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return token;
    }
}
