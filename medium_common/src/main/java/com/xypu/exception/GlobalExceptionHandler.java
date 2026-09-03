package com.xypu.exception;

import com.xypu.response.ResponseVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseVO<Void> handleBusinessException(BusinessException e) {
        logger.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return ResponseVO.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseVO<Void> handleException(Exception e) {
        logger.error("系统异常", e);
        return ResponseVO.error(ErrorCodeEnum.CODE_500.getCode(), ErrorCodeEnum.CODE_500.getMsg());
    }
}
