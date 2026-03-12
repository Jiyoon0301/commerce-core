package com.commercecore.order.service;

import com.commercecore.global.exception.BusinessException;
import com.commercecore.global.exception.ErrorCode;
import com.commercecore.member.domain.Member;
import com.commercecore.member.repository.MemberRepository;
import com.commercecore.order.domain.Order;
import com.commercecore.order.domain.OrderItem;
import com.commercecore.order.domain.OrderStatus;
import com.commercecore.order.dto.OrderCreateRequest;
import com.commercecore.order.dto.OrderItemRequest;
import com.commercecore.order.dto.OrderResponse;
import com.commercecore.order.repository.OrderRepository;
import com.commercecore.product.domain.Product;
import com.commercecore.product.repository.ProductRepository;
import com.commercecore.stock.service.RedisStockService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final RedisStockService redisStockService;

    @Transactional
    public Order createPendingOrder(Long memberId, List<OrderCreateRequest.OrderItemDto> itemRequests) {
        // 1. 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        // 2. 상품 정보 조회 및 OrderItem 생성
        List<OrderItem> orderItems = itemRequests.stream()
                .map(dto -> { // 변수명도 dto로 바꾸면 가독성이 좋습니다.
                    Product product = productRepository.findById(dto.getProductId())
                            .orElseThrow(() -> new IllegalArgumentException("상품이 없습니다."));

                    // DB 가격 사용
                    return OrderItem.create(
                            product.getId(),
                            product.getPrice().intValue(), // dto.getOrderPrice() 대신 이거!
                            dto.getQuantity()
                    );
                })
                .collect(Collectors.toList());

        // 3. 주문 생성
        Order order = Order.create(member, orderItems);

        return orderRepository.save(order);
    }

    @Transactional // 3단계 (성공): 상태만 바꾸고 트랜잭션 종료
    public void markPaid(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        order.markPaid();
    }

    @Transactional
    public void cancel(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 1. 도메인 로직: 상태 변경 (CANCELLED) 및 검증
        order.cancel();

        // 2. 부수 효과: Redis 재고 복구
        // 주문 항목들을 돌며 차감됐던 수량만큼 다시 늘려줌
        order.getOrderItems().forEach(item -> {
            redisStockService.increase(item.getProductId(), item.getQuantity());
        });

        System.out.println("[TEST] 주문 취소 완료 - 재고 복구 실행됨 (주문ID: " + orderId + ")");
    }

    @Transactional
    public void markPaymentPending(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다. ID: " + orderId));

        // 주문 상태를 '결제 대기(판단 보류)'로 변경
        order.updateStatus(OrderStatus.PENDING_PAYMENT);

        log.info("주문 {}의 상태가 PAYMENT_PENDING으로 변경되었습니다. (재조회 대상)", orderId);
    }

    @Transactional(readOnly = true)
    public List<Order> findAllByStatus(OrderStatus status) {
        return orderRepository.findAllByStatus(status);
    }

    // OrderRepository에 List<Order> findAllByMemberId(Long memberId) 가 선언되어 있어야 합니다.
    @Transactional(readOnly = true)
    public List<OrderResponse> findAllByMemberId(Long memberId) {
        return orderRepository.findAllByMemberId(memberId).stream()
                .map(OrderResponse::new)
                .collect(Collectors.toList());
    }
}
