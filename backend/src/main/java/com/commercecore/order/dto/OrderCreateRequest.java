package com.commercecore.order.dto;

import com.commercecore.order.domain.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {
    private Long memberId;
    private List<OrderItemDto> items;

    public List<OrderItemDto> getItems() {
        return items;
    }

    public static OrderCreateRequest of(Long memberId, Long productId, int quantity) {
        return new OrderCreateRequest(
                memberId, // 첫 번째 인자 추가
                List.of(new OrderItemDto(productId, quantity)) // 두 번째 인자
        );
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDto {
        private Long productId;
//        private int orderPrice;
        private int quantity;
    }
}