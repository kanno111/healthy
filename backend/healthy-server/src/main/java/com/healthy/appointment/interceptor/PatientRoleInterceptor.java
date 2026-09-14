package com.healthy.appointment.interceptor;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 限制患者端接口只能由 PATIENT 角色访问。 */
@Component
public class PatientRoleInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object role = request.getAttribute(JwtAuthInterceptor.CURRENT_USER_ROLE);
        if (!"PATIENT".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return true;
    }
}
