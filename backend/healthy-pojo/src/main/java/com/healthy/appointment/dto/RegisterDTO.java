package com.healthy.appointment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 公开患者注册请求。管理员账号由管理端创建。 */
@Data
public class RegisterDTO {
    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 50, message = "账号长度应为 4 到 50 个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 72, message = "密码长度应为 6 到 72 个字符")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名长度不能超过 50 个字符")
    private String name;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "请输入 11 位中国大陆手机号")
    private String phone;

    @NotNull(message = "请选择性别")
    @Min(value = 1, message = "性别只能选择男或女")
    @Max(value = 2, message = "性别只能选择男或女")
    private Integer gender;
}
