package com.healthy.appointment.interceptor;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Restricts doctor endpoints to authenticated DOCTOR accounts. */
@Component
public class DoctorRoleInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object role = request.getAttribute(JwtAuthInterceptor.CURRENT_USER_ROLE);
        if (!"DOCTOR".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return true;
    }
}
