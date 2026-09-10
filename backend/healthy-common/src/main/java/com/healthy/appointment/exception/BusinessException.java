package com.healthy.appointment.exception;

import com.healthy.appointment.enumeration.ErrorCode;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    public BusinessException(ErrorCode errorCode) { super(errorCode.message()); this.errorCode = errorCode; }
    public ErrorCode getErrorCode() { return errorCode; }
}
