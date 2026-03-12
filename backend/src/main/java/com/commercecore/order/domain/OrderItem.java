package com.commercecore.order.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {
    @Id @GeneratedValue
    private Long id;

    private Long productId;
    private int orderPrice;
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private OrderItem(Long productId, int orderPrice, int quantity) {
        this.productId = productId;
        this.orderPrice = orderPrice;
        this.quantity = quantity;
    }

    public static OrderItem create(Long productId, int orderPrice, int quantity) {
        return new OrderItem(productId, orderPrice, quantity);
    }

    public static OrderItem of(Long productId, int orderPrice, int quantity) {
        return new OrderItem(productId, orderPrice, quantity);
    }

    // Order와 연결해주는 편의 메서드 (Order.create에서 사용)
    public void setOrder(Order order) {
        this.order = order;
    }

    public int getTotalPrice() {
        return orderPrice * quantity;
    }

}
