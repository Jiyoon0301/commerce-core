package com.commercecore.order.domain;

import com.commercecore.global.domain.BaseEntity;
import com.commercecore.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders") // order는 SQL 예약어라 관례상 orders로 명명
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member; // 이제 Long memberId 대신 실제 객체를 참조

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private int totalPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    private Order(Member member, int totalPrice) {
        this.member = member; // 이제 타입이 Member 객체여야 합니다.
        this.totalPrice = totalPrice;
        this.status = OrderStatus.PENDING_PAYMENT;
    }

    public static Order create(Member member, List<OrderItem> items) {
        int totalPrice = items.stream()
                .mapToInt(OrderItem::getTotalPrice)
                .sum();

        Order order = new Order(member, totalPrice);
        for (OrderItem item : items) {
            order.addOrderItem(item);
        }
        return order;
    }

    // 연관관계 편의 메서드
    private void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public void markPaid() {
        if (this.status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Invalid state transition");
        }
        this.status = OrderStatus.PAID;
    }

    public void cancel() {
        if (this.status == OrderStatus.PAID) {
            throw new IllegalStateException("Already paid order cannot cancel");
        }
        this.status = OrderStatus.CANCELLED;
    }

    public void updateStatus(OrderStatus newStatus) {
        // 1. 상태 변경 전 검증
        validateTransition(newStatus);
        // 2. 변경
        this.status = newStatus;
    }

    private void validateTransition(OrderStatus newStatus) {
        if (this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 주문은 변경 불가합니다.");
        }
        // 예: 결제 완료 후에는 배송 완료로만 갈 수 있다던가 하는 비즈니스 규칙 추가 가능
        if (this.status == OrderStatus.PAID && newStatus == OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("결제 완료된 주문을 다시 결제 대기로 되돌릴 수 없습니다.");
        }
    }
    // 주문 총액 계산
    public int getTotalAmount() {
        // 주문 항목(OrderItem)들의 (가격 * 수량)을 모두 더함
        return orderItems.stream()
                .mapToInt(item -> item.getOrderPrice() * item.getQuantity())
                .sum();
    }
}
