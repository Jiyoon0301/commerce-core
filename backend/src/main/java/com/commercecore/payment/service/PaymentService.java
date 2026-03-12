package com.commercecore.payment.service;

import com.commercecore.order.domain.Order;
import com.commercecore.payment.domain.Payment;
import com.commercecore.payment.domain.PaymentStatus;
import com.commercecore.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.SocketTimeoutException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentStatus process(Order order, String idempotencyKey) {
        System.out.println("🚩 [TEST] process 시작 - 멱등키: " + idempotencyKey); // 로그 대신 출력
        // 1. 이미 진행 중인 결제가 있는지 조회 또는 생성 (기존 유지)
        Payment payment = paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> paymentRepository.save(Payment.ready(order.getId(), order.getTotalAmount(), idempotencyKey)));
        System.out.println("🚩 [TEST] Payment ID: " + payment.getId() + ", 현재상태: " + payment.getStatus());

        // 2. 이미 성공/실패한 결제면 바로 종료
        if (payment.getStatus() == PaymentStatus.SUCCESS) return PaymentStatus.SUCCESS;
        if (payment.getStatus() == PaymentStatus.FAIL) return PaymentStatus.FAIL;

        // 🚩 3. 핵심: READY -> PROCESSING 원자적 상태 전이 (동시성 방어)
        // DB에서 직접 status가 READY일 때만 PROCESSING으로 바꿉니다.
        int updated = paymentRepository.markProcessingIfReady(payment.getId());
        System.out.println("🚩 [TEST] 업데이트 결과(updated): " + updated);

        if (updated == 0) {
            // 이미 다른 스레드가 PROCESSING으로 바꿨거나 처리 중인 경우
            log.info("이미 결제가 처리 중입니다. (멱등키: {})", idempotencyKey);
            log.info("로직 중단: 상태 전이 실패. 현재 상태: {}", payment.getStatus());
            return payment.getStatus();
        }

        try {
            // 4. 실제 외부 API 호출 (이제 이 부분은 전 세계에서 이 멱등키로 딱 한 번만 실행됨)
            PaymentStatus result = externalCall(order);

            if (result == PaymentStatus.SUCCESS) {
                payment.success("PG_TX_ID_12345");
            } else if (result == PaymentStatus.FAIL) {
                payment.fail();
            }
            return result;

        } catch (java.net.SocketTimeoutException | java.net.ConnectException e) {
            log.error("PG 타임아웃 발생: {}", e.getMessage());
            payment.unknown();
            return PaymentStatus.UNKNOWN;
        } catch (Exception e) {
            log.error("기타 예외 발생: {}", e.getMessage());
            payment.fail();
            return PaymentStatus.FAIL;
        }
    }

    private PaymentStatus externalCall(Order order) throws Exception { // 🚩 Order 파라미터 확인!
        // 실제 PG사 연동 로직이 들어갈 자리입니다.
        // 테스트를 위해 일단 성공을 반환하도록 둡니다.
        return PaymentStatus.SUCCESS;
    }

    @Transactional
    public Payment createOrGet(Long orderId, int amount, String key) {

        return paymentRepository.findByIdempotencyKey(key)
                .orElseGet(() -> {
                    Payment payment = Payment.ready(orderId, amount, key);
                    return paymentRepository.save(payment);
                });
    }

    // UNKNOWN 상태를 복구하기 위해 PG사에 결제 상태를 재조회하는 로직
    public PaymentStatus checkStatus(Order order) {
        try {
            // 1. 실무에서는 여기서 PG사의 '결제 상태 조회 API'를 호출합니다.
            // PG사로부터 받은 응답에 따라 SUCCESS, FAIL 등을 판단합니다.
            PaymentStatus status = externalCheckCall(order);

            // 2. 조회 결과에 따라 DB의 Payment 상태도 업데이트해주는 것이 좋습니다.
            // (이 부분은 나중에 상세히 구현하더라도 일단 틀을 잡아둡니다.)
            return status;

        } catch (Exception e) {
            log.error("결제 상태 재조회 중 에러 발생 (주문번호: {}): {}", order.getId(), e.getMessage());
            return PaymentStatus.UNKNOWN; // 에러 나면 다음 스케줄러 주기 때 다시 확인
        }
    }

    private PaymentStatus externalCheckCall(Order order) {
        // 테스트용 시뮬레이션: 일단은 SUCCESS를 반환하게 설정합니다.
        return PaymentStatus.SUCCESS;
    }
}