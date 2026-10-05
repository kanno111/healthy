package com.healthy.appointment.domain.doctor.remote;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.result.ApiResponse;
import com.healthy.appointment.result.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeignDoctorDirectoryTest {
    @Mock
    private DoctorServiceClient doctorServiceClient;

    @InjectMocks
    private FeignDoctorDirectory doctorDirectory;

    @Test
    void mapsRemoteDoctorToDomainSummary() {
        when(doctorServiceClient.getDoctor(8L)).thenReturn(ApiResponse.success(doctor(8L)));

        var result = doctorDirectory.requireDoctor(8L);

        assertThat(result.getId()).isEqualTo(8L);
        assertThat(result.getUserId()).isEqualTo(108L);
        assertThat(result.getDepartmentId()).isEqualTo(1L);
        assertThat(result.getDepartmentStatus()).isEqualTo(1);
    }

    @Test
    void loadsEnabledDoctorByUserId() {
        when(doctorServiceClient.getEnabledDoctorByUserId(108L))
                .thenReturn(ApiResponse.success(doctor(8L)));

        assertThat(doctorDirectory.requireEnabledDoctorByUserId(108L).getId()).isEqualTo(8L);
    }

    @Test
    void pagesVisibleDoctorsForPatientResourceAggregation() {
        when(doctorServiceClient.pageVisibleDoctors(1L, "heart", 2, 10))
                .thenReturn(ApiResponse.success(PageResult.of(List.of(doctor(8L)), 11, 2, 10)));

        var result = doctorDirectory.pageVisibleDoctors(1L, "heart", 2, 10);

        assertThat(result.records()).singleElement()
                .extracting(doctor -> doctor.getId())
                .isEqualTo(8L);
        assertThat(result.total()).isEqualTo(11);
        assertThat(result.page()).isEqualTo(2);
    }

    @Test
    void rejectsDoctorHiddenFromPatients() {
        DoctorRemoteView doctor = doctor(8L);
        doctor.setDepartmentStatus(0);
        when(doctorServiceClient.getDoctor(8L)).thenReturn(ApiResponse.success(doctor));

        assertThatThrownBy(() -> doctorDirectory.requireVisibleDoctor(8L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void splitsLargeBatchIntoRequestsOfAtMostOneHundredIds() {
        when(doctorServiceClient.listDoctorsByIds(anyList())).thenAnswer(invocation -> {
            List<Long> ids = invocation.getArgument(0);
            return ApiResponse.success(ids.stream().map(this::doctor).toList());
        });
        List<Long> ids = LongStream.rangeClosed(1, 101).boxed().toList();

        Map<Long, ?> result = doctorDirectory.findDoctorsByIds(ids);

        assertThat(result).hasSize(101);
        verify(doctorServiceClient).listDoctorsByIds(ids.subList(0, 100));
        verify(doctorServiceClient).listDoctorsByIds(ids.subList(100, 101));
    }

    @Test
    void skipsRemoteCallForEmptyBatch() {
        assertThat(doctorDirectory.findDoctorsByIds(List.of())).isEmpty();
        verify(doctorServiceClient, never()).listDoctorsByIds(anyList());
    }

    @Test
    void convertsDepartmentDoctorIdsToSet() {
        when(doctorServiceClient.listDoctorIdsByDepartment(1L))
                .thenReturn(ApiResponse.success(List.of(8L, 8L, 9L)));

        assertThat(doctorDirectory.findDoctorIdsByDepartment(1L)).containsExactly(8L, 9L);
    }

    @Test
    void rejectsInvalidSuccessEnvelope() {
        when(doctorServiceClient.getDoctor(8L)).thenReturn(new ApiResponse<>(0, "success", null));

        assertThatThrownBy(() -> doctorDirectory.requireDoctor(8L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
    }

    private DoctorRemoteView doctor(Long id) {
        DoctorRemoteView doctor = new DoctorRemoteView();
        doctor.setId(id);
        doctor.setUserId(id + 100);
        doctor.setName("Doctor " + id);
        doctor.setGender(1);
        doctor.setStatus(1);
        doctor.setDepartmentId(1L);
        doctor.setDepartmentName("Cardiology");
        doctor.setDepartmentStatus(1);
        doctor.setTitle("Attending physician");
        return doctor;
    }
}
