package com.healthy.doctor.interceptor;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.security.AuthenticatedUserHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayIdentityInterceptorTest {
    private final GatewayIdentityInterceptor interceptor = new GatewayIdentityInterceptor();

    @Test
    void convertsGatewayHeadersToRequestAttributes() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthenticatedUserHeaders.USER_ID, "8");
        request.addHeader(AuthenticatedUserHeaders.ROLE, " STAFF ");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
        assertThat(request.getAttribute(GatewayIdentityInterceptor.CURRENT_USER_ID)).isEqualTo(8L);
        assertThat(request.getAttribute(GatewayIdentityInterceptor.CURRENT_USER_ROLE)).isEqualTo("STAFF");
    }

    @Test
    void rejectsMissingGatewayIdentity() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> interceptor.preHandle(
                request, new MockHttpServletResponse(), new Object()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void rejectsInvalidUserIdHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthenticatedUserHeaders.USER_ID, "not-a-number");
        request.addHeader(AuthenticatedUserHeaders.ROLE, "STAFF");

        assertThatThrownBy(() -> interceptor.preHandle(
                request, new MockHttpServletResponse(), new Object()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }
}
