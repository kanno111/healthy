package com.healthy.doctor.controller;

import com.healthy.appointment.dto.DepartmentSaveDTO;
import com.healthy.appointment.security.AuthenticatedUserHeaders;
import com.healthy.appointment.vo.DepartmentVO;
import com.healthy.doctor.handler.DoctorExceptionHandler;
import com.healthy.doctor.interceptor.GatewayIdentityInterceptor;
import com.healthy.doctor.interceptor.GatewayStaffRoleInterceptor;
import com.healthy.doctor.service.DepartmentCommandService;
import com.healthy.doctor.service.DepartmentQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class AdminDepartmentControllerTest {
    @Mock
    private DepartmentQueryService departmentQueryService;

    @Mock
    private DepartmentCommandService departmentCommandService;

    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(new AdminDepartmentController(
                        departmentQueryService, departmentCommandService))
                .addInterceptors(
                        new GatewayIdentityInterceptor(),
                        new GatewayStaffRoleInterceptor())
                .setControllerAdvice(new DoctorExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void staffCanListDepartmentsWithExistingContract() throws Exception {
        DepartmentVO department = new DepartmentVO();
        department.setId(1L);
        department.setName("Cardiology");
        department.setDoctorCount(6L);
        when(departmentQueryService.list("Card", 1)).thenReturn(List.of(department));

        mockMvc.perform(get("/admin/departments")
                        .header(AuthenticatedUserHeaders.USER_ID, "100")
                        .header(AuthenticatedUserHeaders.ROLE, "STAFF")
                        .param("name", "Card")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].doctorCount").value(6));
    }

    @Test
    void staffCanCreateDepartmentWithExistingContract() throws Exception {
        when(departmentCommandService.create(any(DepartmentSaveDTO.class))).thenReturn(9L);

        mockMvc.perform(post("/admin/departments")
                        .header(AuthenticatedUserHeaders.USER_ID, "100")
                        .header(AuthenticatedUserHeaders.ROLE, "STAFF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Neurology",
                                  "description": "Neurology department",
                                  "sortOrder": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(9));
    }

    @Test
    void rejectsRequestWithoutGatewayIdentity() throws Exception {
        mockMvc.perform(get("/admin/departments"))
                .andExpect(status().isUnauthorized());

        verify(departmentQueryService, never()).list(any(), any());
    }

    @Test
    void rejectsNonStaffRole() throws Exception {
        mockMvc.perform(get("/admin/departments")
                        .header(AuthenticatedUserHeaders.USER_ID, "100")
                        .header(AuthenticatedUserHeaders.ROLE, "PATIENT"))
                .andExpect(status().isForbidden());
    }

    @Test
    void preservesValidationErrorResponse() throws Exception {
        mockMvc.perform(post("/admin/departments")
                        .header(AuthenticatedUserHeaders.USER_ID, "100")
                        .header(AuthenticatedUserHeaders.ROLE, "STAFF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").isNumber());

        verify(departmentCommandService, never()).create(any());
    }
}
