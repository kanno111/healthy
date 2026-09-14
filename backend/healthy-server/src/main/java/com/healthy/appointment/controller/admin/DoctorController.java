package com.healthy.appointment.controller.admin;

import com.healthy.appointment.dto.DoctorSaveDTO;
import com.healthy.appointment.result.ApiResponse;
import com.healthy.appointment.result.PageResult;
import com.healthy.appointment.service.DoctorService;
import com.healthy.appointment.vo.DoctorVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/doctors")
@RequiredArgsConstructor
public class DoctorController {
    private final DoctorService doctorService;

    @GetMapping
    /** 分页查询医生，可按姓名、所属科室和启停状态筛选。 */
    public ApiResponse<PageResult<DoctorVO>> page(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer status,
            @RequestParam int page,
            @RequestParam int pageSize
    ) {
        return ApiResponse.success(doctorService.page(name, departmentId, status, page, pageSize));
    }

    @GetMapping("/{id}")
    /** 查询单个医生详情，供编辑页面回显。 */
    public ApiResponse<DoctorVO> getById(@PathVariable Long id) {
        return ApiResponse.success(doctorService.getById(id));
    }

    @PostMapping
    /** 新建医生档案，成功后返回新记录 ID。 */
    public ApiResponse<Long> create(@Valid @RequestBody DoctorSaveDTO doctorSaveDTO) {
        return ApiResponse.success(doctorService.create(doctorSaveDTO));
    }

    @PutMapping("/{id}")
    /** 修改指定医生的基础资料和所属科室。 */
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody DoctorSaveDTO doctorSaveDTO) {
        doctorService.update(id, doctorSaveDTO);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/status")
    /** 启用或停用医生；保留历史排班和预约数据。 */
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        doctorService.updateStatus(id, status);
        return ApiResponse.success();
    }
}
