package com.healthy.doctor.interceptor;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Restricts doctor-service patient APIs to authenticated PATIENT users. */
@Component
public class GatewayPatientRoleInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object role = request.getAttribute(GatewayIdentityInterceptor.CURRENT_USER_ROLE);
        if (!"PATIENT".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return true;
    }
}
