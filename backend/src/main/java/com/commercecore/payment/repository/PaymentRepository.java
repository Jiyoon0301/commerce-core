package com.commercecore.payment.repository;

import com.commercecore.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Modifying
    @Query("update Payment p set p.status = 'PROCESSING' where p.id = :id and p.status = 'READY'")
    int markProcessingIfReady(@Param("id") Long id);

    // 멱등 키로 결제 내역을 찾는 기능이 꼭 필요합니다.
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
