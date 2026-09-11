package com.healthy.appointment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DoctorSaveDTO {
    @NotBlank(message = "医生姓名不能为空")
    @Size(max = 50, message = "医生姓名不能超过50个字符")
    private String name;

    @NotNull(message = "医生性别不能为空")
    @Min(value = 1, message = "医生性别只能为1或2")
    @Max(value = 2, message = "医生性别只能为1或2")
    private Integer gender;

    @NotNull(message = "所属科室不能为空")
    private Long departmentId;

    @NotBlank(message = "医生工号不能为空")
    @Size(max = 32, message = "医生工号不能超过32个字符")
    private String doctorCode;

    @Size(max = 50, message = "医生职称不能超过50个字符")
    private String title;

    @Size(max = 5000, message = "医生简介不能超过5000个字符")
    private String introduction;

    @Size(max = 255, message = "头像地址不能超过255个字符")
    private String avatarUrl;

    @Min(value = 0, message = "排序值不能小于0")
    private Integer sortOrder;
}
