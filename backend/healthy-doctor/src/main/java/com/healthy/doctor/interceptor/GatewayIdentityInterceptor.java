package com.healthy.doctor.interceptor;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.security.AuthenticatedUserHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Converts identity headers verified by the gateway into request attributes. */
@Component
public class GatewayIdentityInterceptor implements HandlerInterceptor {
    public static final String CURRENT_USER_ID = "currentUserId";
    public static final String CURRENT_USER_ROLE = "currentUserRole";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userIdHeader = request.getHeader(AuthenticatedUserHeaders.USER_ID);
        String roleHeader = request.getHeader(AuthenticatedUserHeaders.ROLE);
        if (userIdHeader == null || roleHeader == null || roleHeader.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        try {
            Long userId = Long.valueOf(userIdHeader);
            if (userId < 1) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            request.setAttribute(CURRENT_USER_ID, userId);
            request.setAttribute(CURRENT_USER_ROLE, roleHeader.trim());
            return true;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
