package com.commercecore.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderItemRequest {
    private Long productId;
    private int quantity;
}