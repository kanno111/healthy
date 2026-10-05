package com.healthy.doctor.controller;

import com.healthy.appointment.dto.DoctorSaveDTO;
import com.healthy.appointment.result.PageResult;
import com.healthy.appointment.security.AuthenticatedUserHeaders;
import com.healthy.appointment.vo.DoctorVO;
import com.healthy.doctor.handler.DoctorExceptionHandler;
import com.healthy.doctor.interceptor.GatewayIdentityInterceptor;
import com.healthy.doctor.interceptor.GatewayStaffRoleInterceptor;
import com.healthy.doctor.service.DoctorCommandService;
import com.healthy.doctor.service.DoctorQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;

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
class AdminDoctorControllerTest {
    @Mock
    private DoctorQueryService doctorQueryService;

    @Mock
    private DoctorCommandService doctorCommandService;

    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(new AdminDoctorController(doctorQueryService, doctorCommandService))
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
    void staffCanPageDoctorsWithExistingContract() throws Exception {
        DoctorVO doctor = new DoctorVO();
        doctor.setId(8L);
        doctor.setName("Doctor Zhang");
        when(doctorQueryService.page("Zhang", 1L, 1, 2, 10))
                .thenReturn(PageResult.of(List.of(doctor), 11, 2, 10));

        mockMvc.perform(get("/admin/doctors")
                        .header(AuthenticatedUserHeaders.USER_ID, "100")
                        .header(AuthenticatedUserHeaders.ROLE, "STAFF")
                        .param("name", "Zhang")
                        .param("departmentId", "1")
                        .param("status", "1")
                        .param("page", "2")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].id").value(8))
                .andExpect(jsonPath("$.data.total").value(11))
                .andExpect(jsonPath("$.data.page").value(2));
    }

    @Test
    void staffCanCreateDoctorWithExistingContract() throws Exception {
        when(doctorCommandService.create(any(DoctorSaveDTO.class))).thenReturn(9L);

        mockMvc.perform(post("/admin/doctors")
                        .header(AuthenticatedUserHeaders.USER_ID, "100")
                        .header(AuthenticatedUserHeaders.ROLE, "STAFF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Doctor Li",
                                  "gender": 1,
                                  "departmentId": 2,
                                  "doctorCode": "D-009",
                                  "sortOrder": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(9));
    }

    @Test
    void rejectsRequestWithoutGatewayIdentity() throws Exception {
        mockMvc.perform(get("/admin/doctors")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isUnauthorized());

        verify(doctorQueryService, never()).page(any(), any(), any(), any(Integer.class), any(Integer.class));
    }

    @Test
    void rejectsNonStaffRole() throws Exception {
        mockMvc.perform(get("/admin/doctors")
                        .header(AuthenticatedUserHeaders.USER_ID, "100")
                        .header(AuthenticatedUserHeaders.ROLE, "PATIENT")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void preservesValidationErrorResponse() throws Exception {
        mockMvc.perform(post("/admin/doctors")
                        .header(AuthenticatedUserHeaders.USER_ID, "100")
                        .header(AuthenticatedUserHeaders.ROLE, "STAFF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").isNumber());

        verify(doctorCommandService, never()).create(any());
    }
}
