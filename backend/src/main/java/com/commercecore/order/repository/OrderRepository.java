package com.commercecore.order.repository;

import com.commercecore.order.domain.Order;
import com.commercecore.order.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // OrderRepository.java (인터페이스) 내부에 추가
    List<Order> findAllByStatus(OrderStatus status);

    List<Order> findAllByMemberId(Long memberId);
}
