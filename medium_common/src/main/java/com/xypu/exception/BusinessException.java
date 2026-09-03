package com.xypu.exception;

public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum.getMsg());
        this.code = errorCodeEnum.getCode();
    }

    public BusinessException(String message) {
        super(message);
        this.code = ErrorCodeEnum.CODE_500.getCode();
    }

    public Integer getCode() {
        return code;
    }
}
