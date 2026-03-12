package com.commercecore.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 멱등성 에러(이미 주문함) 발생 시 409 Conflict를 리턴
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException e) {
        String message = e.getMessage();

        // 재고 부족은 보통 400 Bad Request가 더 일반적
        if (message.contains("재고 부족")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
        }

        // 멱등성 에러는 기존대로 409
        return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
    }
}