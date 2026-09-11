package com.healthy.appointment.controller.admin;

import com.healthy.appointment.dto.DepartmentSaveDTO;
import com.healthy.appointment.result.ApiResponse;
import com.healthy.appointment.service.DepartmentService;
import com.healthy.appointment.vo.DepartmentVO;
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

import java.util.List;

@RestController
@RequestMapping("/admin/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService departmentService;

    @GetMapping
    /** 查询科室列表，可按名称和启停状态筛选。 */
    public ApiResponse<List<DepartmentVO>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status
    ) {
        return ApiResponse.success(departmentService.list(name, status));
    }

    @GetMapping("/{id}")
    /** 查询单个科室详情，供编辑页面回显。 */
    public ApiResponse<DepartmentVO> getById(@PathVariable Long id) {
        return ApiResponse.success(departmentService.getById(id));
    }

    @PostMapping
    /** 新建科室，成功后返回新记录 ID。 */
    public ApiResponse<Long> create(@Valid @RequestBody DepartmentSaveDTO departmentSaveDTO) {
        return ApiResponse.success(departmentService.create(departmentSaveDTO));
    }

    @PutMapping("/{id}")
    /** 修改指定科室的名称、简介和排序值。 */
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody DepartmentSaveDTO departmentSaveDTO) {
        departmentService.update(id, departmentSaveDTO);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/status")
    /** 启用或停用科室；不进行物理删除。 */
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        departmentService.updateStatus(id, status);
        return ApiResponse.success();
    }
}
