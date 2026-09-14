package com.healthy.appointment.service.impl;

import com.healthy.appointment.dto.AppointmentCreateDTO;
import com.healthy.appointment.entity.Appointment;
import com.healthy.appointment.entity.DoctorScheduleSlot;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.AppointmentMapper;
import com.healthy.appointment.mapper.DoctorScheduleSlotMapper;
import com.healthy.appointment.mapper.PatientMapper;
import com.healthy.appointment.service.AppointmentService;
import com.healthy.appointment.vo.PatientAppointmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.healthy.appointment.constant.Constant.APPOINTMENT_STATUS_CONFIRMED;
import static com.healthy.appointment.constant.Constant.SCHEDULE_STATUS_OPEN;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {
    private final PatientMapper patientMapper;
    private final DoctorScheduleSlotMapper doctorScheduleSlotMapper;
    private final AppointmentMapper appointmentMapper;

    /** 创建最小预约记录，并同步扣减对应班次的剩余号源。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PatientAppointmentVO create(Long userId, AppointmentCreateDTO appointmentCreateDTO) {
        Long patientId = findEnabledPatientId(userId);
        DoctorScheduleSlot slot = getAvailableSlot(appointmentCreateDTO.getScheduleSlotId());
        if (doctorScheduleSlotMapper.decreaseRemainingCapacity(slot.getId()) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        Appointment appointment = new Appointment();
        appointment.setAppointmentNo(UUID.randomUUID().toString().replace("-", ""));
        appointment.setPatientId(patientId);
        appointment.setDoctorId(slot.getDoctorId());
        appointment.setScheduleSlotId(slot.getId());
        appointment.setScheduleDate(slot.getScheduleDate());
        appointment.setStartTime(slot.getStartTime());
        appointment.setEndTime(slot.getEndTime());
        appointment.setStatus(APPOINTMENT_STATUS_CONFIRMED);
        appointmentMapper.insert(appointment);

        return appointmentMapper.findByIdAndPatientId(appointment.getId(), patientId);
    }

    /** 查询当前登录患者的预约记录，最新创建的记录排在最前。 */
    @Override
    public List<PatientAppointmentVO> listMyAppointments(Long userId) {
        return appointmentMapper.listByPatientId(findEnabledPatientId(userId));
    }

    private Long findEnabledPatientId(Long userId) {
        Long patientId = patientMapper.findEnabledIdByUserId(userId);
        if (patientId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return patientId;
    }

    private DoctorScheduleSlot getAvailableSlot(Long slotId) {
        DoctorScheduleSlot slot = doctorScheduleSlotMapper.findById(slotId);
        if (slot == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDate scheduleDate = slot.getScheduleDate();
        if (!SCHEDULE_STATUS_OPEN.equals(slot.getStatus())
                || slot.getRemainingCapacity() < 1
                || scheduleDate.isBefore(now.toLocalDate())
                || (scheduleDate.equals(now.toLocalDate()) && !slot.getEndTime().isAfter(now.toLocalTime()))) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        return slot;
    }
}
