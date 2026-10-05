package com.healthy.identity.interceptor;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.security.AuthenticatedUserHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class GatewayIdentityInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userIdHeader = request.getHeader(AuthenticatedUserHeaders.USER_ID);
        String roleHeader = request.getHeader(AuthenticatedUserHeaders.ROLE);
        if (userIdHeader == null || roleHeader == null || roleHeader.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            if (Long.parseLong(userIdHeader) < 1) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            return true;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
