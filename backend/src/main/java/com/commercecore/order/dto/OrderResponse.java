package com.commercecore.order.dto;

import com.commercecore.order.domain.Order;
import com.commercecore.order.domain.OrderStatus;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class OrderResponse {
    private Long id;
    private OrderStatus status;
    private int totalPrice;
    private LocalDateTime orderDate;

    public OrderResponse(Order order) {
        this.id = order.getId();
        this.status = order.getStatus();
        this.totalPrice = order.getTotalPrice(); // Order 엔티티에 해당 필드나 계산 로직이 있어야 함
        this.orderDate = order.getCreatedAt();
    }
}