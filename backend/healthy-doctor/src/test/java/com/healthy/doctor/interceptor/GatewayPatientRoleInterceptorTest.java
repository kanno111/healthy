package com.healthy.doctor.interceptor;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayPatientRoleInterceptorTest {
    private final GatewayPatientRoleInterceptor interceptor = new GatewayPatientRoleInterceptor();

    @Test
    void allowsPatientUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(GatewayIdentityInterceptor.CURRENT_USER_ROLE, "PATIENT");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void rejectsNonPatientUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(GatewayIdentityInterceptor.CURRENT_USER_ROLE, "STAFF");

        assertThatThrownBy(() -> interceptor.preHandle(
                request, new MockHttpServletResponse(), new Object()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }
}
