package com.healthy.doctor.controller;

import com.healthy.appointment.security.AuthenticatedUserHeaders;
import com.healthy.doctor.handler.DoctorExceptionHandler;
import com.healthy.doctor.interceptor.GatewayIdentityInterceptor;
import com.healthy.doctor.interceptor.GatewayPatientRoleInterceptor;
import com.healthy.doctor.service.DepartmentQueryService;
import com.healthy.doctor.vo.PatientDepartmentVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class PatientDepartmentControllerTest {
    @Mock
    private DepartmentQueryService departmentQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new PatientDepartmentController(departmentQueryService))
                .addInterceptors(
                        new GatewayIdentityInterceptor(),
                        new GatewayPatientRoleInterceptor())
                .setControllerAdvice(new DoctorExceptionHandler())
                .build();
    }

    @Test
    void patientCanListVisibleDepartmentsWithoutAdminFields() throws Exception {
        PatientDepartmentVO department = new PatientDepartmentVO();
        department.setId(1L);
        department.setName("Cardiology");
        department.setDescription("Heart care");
        when(departmentQueryService.listVisible()).thenReturn(List.of(department));

        mockMvc.perform(get("/user/departments")
                        .header(AuthenticatedUserHeaders.USER_ID, "200")
                        .header(AuthenticatedUserHeaders.ROLE, "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Cardiology"))
                .andExpect(jsonPath("$.data[0].status").doesNotExist())
                .andExpect(jsonPath("$.data[0].doctorCount").doesNotExist());
    }

    @Test
    void rejectsRequestWithoutGatewayIdentity() throws Exception {
        mockMvc.perform(get("/user/departments"))
                .andExpect(status().isUnauthorized());

        verify(departmentQueryService, never()).listVisible();
    }

    @Test
    void rejectsNonPatientRole() throws Exception {
        mockMvc.perform(get("/user/departments")
                        .header(AuthenticatedUserHeaders.USER_ID, "100")
                        .header(AuthenticatedUserHeaders.ROLE, "STAFF"))
                .andExpect(status().isForbidden());
    }
}
