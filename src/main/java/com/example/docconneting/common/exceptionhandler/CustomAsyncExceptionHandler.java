package com.example.docconneting.common.exceptionhandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

@Slf4j
public class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... obj) {
        log.info("예외 메시지: {}", ex.getMessage());
        log.info("예외 발생 메서드: {}", method.getName());

        for (Object param : obj) {
            log.info("파라미터 값 : {}", param);
        }
    }
}
