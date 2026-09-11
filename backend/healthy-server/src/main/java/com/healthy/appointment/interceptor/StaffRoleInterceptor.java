package com.healthy.appointment.interceptor;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 限制管理端接口只能由 STAFF 角色访问。防止用户通过拿到自己的token后直接通过其他工具如postman直接访问网站接口 */
@Component
public class StaffRoleInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object role = request.getAttribute(JwtAuthInterceptor.CURRENT_USER_ROLE);
        if (!"STAFF".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return true;
    }
}
