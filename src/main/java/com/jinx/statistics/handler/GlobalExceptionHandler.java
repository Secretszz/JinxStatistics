package com.jinx.statistics.handler;

import com.jinx.statistics.exception.BaseException;
import com.jinx.statistics.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler(BaseException.class)
    public ApiResponse exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return ApiResponse.error(ex.getMessage());
    }
}
