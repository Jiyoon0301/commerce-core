package com.commercecore.order.dto;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class OrderRequest {
    private Long memberId;
    private List<OrderItemRequest> items; // 상품 ID, 수량 등을 담은 리스트
}