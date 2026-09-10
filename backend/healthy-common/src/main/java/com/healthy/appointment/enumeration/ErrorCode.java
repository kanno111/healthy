package com.healthy.appointment.enumeration;

public enum ErrorCode {
    VALIDATION_ERROR(40001, "请求参数不合法"),
    PATIENT_GENDER_REQUIRED(40002, "患者注册必须选择性别"),
    UNAUTHORIZED(40100, "未登录或登录已失效"),
    LOGIN_FAILED(40101, "账号或密码错误"),
    FORBIDDEN(40300, "没有操作权限"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "资源状态冲突"),
    USERNAME_EXISTS(40901, "账号已存在"),
    PHONE_EXISTS(40902, "手机号已存在"),
    INTERNAL_ERROR(50000, "服务器暂时不可用");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
