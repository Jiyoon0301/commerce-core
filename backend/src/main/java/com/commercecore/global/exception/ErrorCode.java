package com.commercecore.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    ORDER_NOT_FOUND("O001", "주문을 찾을 수 없습니다."),
    INVALID_ORDER_STATUS("O002", "유효하지 않은 주문 상태 변경입니다."),
    OUT_OF_STOCK("S001", "재고가 부족합니다.");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}