package com.healthy.doctor.interceptor;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayStaffRoleInterceptorTest {
    private final GatewayStaffRoleInterceptor interceptor = new GatewayStaffRoleInterceptor();

    @Test
    void allowsStaffUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(GatewayIdentityInterceptor.CURRENT_USER_ROLE, "STAFF");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void rejectsNonStaffUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(GatewayIdentityInterceptor.CURRENT_USER_ROLE, "PATIENT");

        assertThatThrownBy(() -> interceptor.preHandle(
                request, new MockHttpServletResponse(), new Object()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }
}
