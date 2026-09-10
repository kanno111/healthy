package com.healthy.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank(message = "账号不能为空")
    @Size(max = 50, message = "账号长度不能超过50个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 72, message = "密码长度不能超过72个字符")
    private String password;
}
