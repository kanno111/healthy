package com.healthy.appointment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DepartmentSaveDTO {
    @NotBlank(message = "科室名称不能为空")
    @Size(max = 50, message = "科室名称不能超过50个字符")
    private String name;

    @Size(max = 500, message = "科室简介不能超过500个字符")
    private String description;

    @Min(value = 0, message = "排序值不能小于0")
    private Integer sortOrder;
}
