package com.healthy.appointment.domain.identity.remote;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.result.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeignPatientDirectoryTest {
    @Mock
    private IdentityServiceClient identityServiceClient;
    @InjectMocks
    private FeignPatientDirectory patientDirectory;

    @Test
    void resolvesEnabledPatientIdByUserId() {
        when(identityServiceClient.getEnabledPatientByUserId(11L))
                .thenReturn(ApiResponse.success(patient(21L)));

        assertThat(patientDirectory.requireEnabledPatientIdByUserId(11L)).isEqualTo(21L);
    }

    @Test
    void splitsLargeBatchIntoRequestsOfAtMostOneHundredIds() {
        when(identityServiceClient.listPatientsByIds(anyList())).thenAnswer(invocation -> {
            List<Long> ids = invocation.getArgument(0);
            return ApiResponse.success(ids.stream().map(this::patient).toList());
        });
        List<Long> ids = LongStream.rangeClosed(1, 101).boxed().toList();

        assertThat(patientDirectory.findByIds(ids)).hasSize(101);
        verify(identityServiceClient).listPatientsByIds(ids.subList(0, 100));
        verify(identityServiceClient).listPatientsByIds(ids.subList(100, 101));
    }

    @Test
    void skipsRemoteCallForEmptyBatchAndBlankKeyword() {
        assertThat(patientDirectory.findByIds(List.of())).isEmpty();
        assertThat(patientDirectory.findIdsByKeyword(" ")).isEmpty();
        verify(identityServiceClient, never()).listPatientsByIds(anyList());
    }

    @Test
    void rejectsInvalidSuccessEnvelope() {
        when(identityServiceClient.getEnabledPatientByUserId(11L))
                .thenReturn(new ApiResponse<>(0, "success", null));

        assertThatThrownBy(() -> patientDirectory.requireEnabledPatientIdByUserId(11L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
    }

    private PatientRemoteView patient(Long id) {
        PatientRemoteView patient = new PatientRemoteView();
        patient.setId(id);
        patient.setUserId(id + 100);
        patient.setName("Patient " + id);
        patient.setUsername("patient_" + id);
        patient.setPhone("13800000000");
        patient.setStatus(1);
        return patient;
    }
}
