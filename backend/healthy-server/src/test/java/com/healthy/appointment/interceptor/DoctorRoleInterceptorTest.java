package com.healthy.appointment.interceptor;

import com.healthy.appointment.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DoctorRoleInterceptorTest {
    private final DoctorRoleInterceptor interceptor = new DoctorRoleInterceptor();
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @Test
    void allowsDoctorRole() {
        HttpServletRequest request = requestWithRole("DOCTOR");

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    @Test
    void rejectsPatientAndStaffRoles() {
        assertThatThrownBy(() -> interceptor.preHandle(requestWithRole("PATIENT"), response, new Object()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> interceptor.preHandle(requestWithRole("STAFF"), response, new Object()))
                .isInstanceOf(BusinessException.class);
    }

    private HttpServletRequest requestWithRole(String role) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(JwtAuthInterceptor.CURRENT_USER_ROLE)).thenReturn(role);
        return request;
    }
}
