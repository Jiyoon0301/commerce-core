package com.commercecore.order.domain;

public enum OrderStatus {
    PENDING_PAYMENT("결제 대기"),
    PAID("결제 완료"),
    PREPARING("상품 준비 중"),
    DELIVERED("배송 완료"),
    CANCELLED("주문 취소");

    private final String description;
    OrderStatus(String description) { this.description = description; }
    public String getDescription() { return description; }
}