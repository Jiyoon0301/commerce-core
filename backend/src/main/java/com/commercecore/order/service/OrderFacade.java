package com.commercecore.order.service;

import com.commercecore.order.domain.Order;
import com.commercecore.order.dto.OrderCreateRequest;
import com.commercecore.payment.domain.PaymentStatus;
import com.commercecore.payment.service.PaymentService;
import com.commercecore.stock.service.RedisStockService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final RedisStockService redisStockService;
    private final OrderService orderService;
    private final PaymentService paymentService;

    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    public Long placeOrder(Long memberId, OrderCreateRequest request, String idempotencyKey) {

        // 1. 멱등성 체크 (Redis에 키가 있는지 확인)
        String lockKey = "idempotency:" + idempotencyKey;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(lockKey, "PROCESSING", Duration.ofMinutes(10));
        if (isNew == null || !isNew) {
            throw new IllegalStateException("이미 처리 중이거나 완료된 주문입니다: " + idempotencyKey);
        }

        Long orderId = null;
        boolean stockDecreased = false; // 🚩 재고 차감 성공 여부 플래그

        try {
            // redis 재고 차감
            request.getItems().forEach(item -> {
                // decrease 메서드 내부에서 결과값을 확인
                redisStockService.decrease(item.getProductId(), item.getQuantity());
            });

            // 주문 생성
            Order order = orderService.createPendingOrder(memberId, request.getItems());
            orderId = order.getId();

            // 결제 처리
            PaymentStatus status = paymentService.process(order, idempotencyKey);

            if (status == PaymentStatus.SUCCESS) {
                System.out.println("🚩 [TEST] 결제 성공 -> markPaid 호출 시도 (주문ID: " + orderId + ")");
                orderService.markPaid(orderId);
            } else if (status == PaymentStatus.FAIL) {
                orderService.cancel(orderId);
                restoreStock(request);
            } else {
                // UNKNOWN 상태는 스케줄러가 해결할 때까지 보류
                orderService.markPaymentPending(orderId);
            }

        } catch (IllegalStateException e) {
            // 🚩 "재고 부족" 에러는 멱등성 보장/주문 생성 과정의 에러가 아님!
            // 재고가 없어서 실패한 것이므로 restoreStock을 하면 절대 안 됨.
            throw e;

        } catch (Exception e) {

            if (orderId != null) {
                orderService.cancel(orderId);
            }

            restoreStock(request);
            throw e;
        }

        return orderId;
    }

    private void restoreStock(OrderCreateRequest request) {
        // 주문 요청에 들어있던 상품들만큼 다시 재고를 +1 시켜줌
        request.getItems().forEach(item ->
                redisStockService.increase(item.getProductId(), item.getQuantity())
        );
    }
}
