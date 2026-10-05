package com.healthy.identity.service;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.identity.mapper.PatientMapper;
import com.healthy.identity.model.PatientQueryView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientQueryServiceTest {
    @Mock
    private PatientMapper patientMapper;
    @InjectMocks
    private PatientQueryService patientQueryService;

    @Test
    void requiresEnabledPatientByUserId() {
        PatientQueryView patient = new PatientQueryView();
        patient.setId(7L);
        when(patientMapper.findEnabledByUserId(3L)).thenReturn(patient);

        assertThat(patientQueryService.requireEnabledByUserId(3L).getId()).isEqualTo(7L);
    }

    @Test
    void reportsMissingPatientAsNotFound() {
        assertThatThrownBy(() -> patientQueryService.requireEnabledByUserId(3L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void skipsMapperForEmptyBatch() {
        assertThat(patientQueryService.findByIds(List.of())).isEmpty();
        verify(patientMapper, never()).listViewsByIds(List.of());
    }
}
