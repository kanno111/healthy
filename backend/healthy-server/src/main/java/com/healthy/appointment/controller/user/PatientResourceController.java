package com.healthy.appointment.controller.user;

import com.healthy.appointment.result.ApiResponse;
import com.healthy.appointment.result.PageResult;
import com.healthy.appointment.service.PatientResourceService;
import com.healthy.appointment.vo.PatientDepartmentVO;
import com.healthy.appointment.vo.PatientDoctorVO;
import com.healthy.appointment.vo.PatientScheduleSlotVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class PatientResourceController {
    private final PatientResourceService patientResourceService;

    @GetMapping("/departments")
    /** 查询患者端可见的所有启用科室。 */
    public ApiResponse<List<PatientDepartmentVO>> listDepartments() {
        return ApiResponse.success(patientResourceService.listDepartments());
    }

    @GetMapping("/doctors")
    /** 分页查询患者端可见的医生，可按科室和关键词筛选。 */
    public ApiResponse<PageResult<PatientDoctorVO>> pageDoctors(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String keyword,
            @RequestParam int page,
            @RequestParam int pageSize
    ) {
        return ApiResponse.success(patientResourceService.pageDoctors(departmentId, keyword, page, pageSize));
    }

    @GetMapping("/doctors/{id}")
    /** 查询启用医生的公开详情，不返回工号等管理字段。 */
    public ApiResponse<PatientDoctorVO> getDoctorById(@PathVariable Long id) {
        return ApiResponse.success(patientResourceService.getDoctorById(id));
    }

    @GetMapping("/doctors/{id}/schedule-slots")
    /** 查询指定医生未来最多 14 天内开放的出诊班次和剩余号源。 */
    public ApiResponse<List<PatientScheduleSlotVO>> listScheduleSlots(
            @PathVariable Long id,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return ApiResponse.success(patientResourceService.listScheduleSlots(id, startDate, endDate));
    }
}
