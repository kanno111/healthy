package com.healthy.appointment.interceptor;

import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.security.AuthenticatedUserHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayIdentityInterceptorTest {
    private final GatewayIdentityInterceptor interceptor = new GatewayIdentityInterceptor();
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @Test
    void convertsTrustedHeadersToRequestAttributes() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(AuthenticatedUserHeaders.USER_ID)).thenReturn("12");
        when(request.getHeader(AuthenticatedUserHeaders.ROLE)).thenReturn("PATIENT");

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        verify(request).setAttribute(GatewayIdentityInterceptor.CURRENT_USER_ID, 12L);
        verify(request).setAttribute(GatewayIdentityInterceptor.CURRENT_USER_ROLE, "PATIENT");
    }

    @Test
    void rejectsMissingOrInvalidHeaders() {
        HttpServletRequest missing = mock(HttpServletRequest.class);
        HttpServletRequest invalid = mock(HttpServletRequest.class);
        when(invalid.getHeader(AuthenticatedUserHeaders.USER_ID)).thenReturn("not-a-number");
        when(invalid.getHeader(AuthenticatedUserHeaders.ROLE)).thenReturn("PATIENT");

        assertThatThrownBy(() -> interceptor.preHandle(missing, response, new Object()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> interceptor.preHandle(invalid, response, new Object()))
                .isInstanceOf(BusinessException.class);
    }
}
