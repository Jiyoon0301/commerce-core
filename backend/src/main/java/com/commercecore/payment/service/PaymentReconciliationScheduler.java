package com.commercecore.payment.service;

import com.commercecore.order.domain.Order;
import com.commercecore.order.domain.OrderStatus; // 상태값 임포트 확인
import com.commercecore.order.service.OrderService;
import com.commercecore.payment.domain.PaymentStatus;
import com.commercecore.stock.service.RedisStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component // 스프링 빈으로 등록해야 스케줄러가 작동함
@RequiredArgsConstructor
public class PaymentReconciliationScheduler {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final RedisStockService redisStockService;

    // 10초마다 실행
    @Scheduled(fixedDelay = 10000)
    public void reconcilePayments() {
        log.info("결제 상태 재확인 스케줄러 실행 중...");

        // 1. '판단 보류' 상태인 주문들만 가져오기
        // (orderRepository를 직접 쓰기보다 orderService를 거치는 게 깔끔합니다)
        List<Order> pendingOrders = orderService.findAllByStatus(OrderStatus.PENDING_PAYMENT);

        for (Order order : pendingOrders) {
            try {
                // 2. PG사에 실제 결제 여부 재조회
                PaymentStatus status = paymentService.checkStatus(order);

                if (status == PaymentStatus.SUCCESS) {
                    orderService.markPaid(order.getId());
                    log.info("주문 {} 결제 확인 완료", order.getId());
                }
                else if (status == PaymentStatus.FAIL) {
                    // 3. 확실히 실패라면 취소 및 재고 복구
                    orderService.cancel(order.getId());

                    // orderItem 리스트를 돌며 재고 복구 (기존 로직 활용)
                    order.getOrderItems().forEach(item ->
                            redisStockService.increase(item.getProductId(), item.getQuantity())
                    );
                    log.info("주문 {} 결제 실패 확인 -> 재고 복구 완료", order.getId());
                }
                // UNKNOWN이면 다음 10초 뒤에 다시 확인함
            } catch (Exception e) {
                log.error("주문 {} 처리 중 에러 발생: {}", order.getId(), e.getMessage());
            }
        }
    }
}