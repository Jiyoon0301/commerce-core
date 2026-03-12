package com.commercecore.payment.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_idempotency", // "멱등성 유니크 키"라고 이름표를 붙임
                        columnNames = "idempotencyKey"
                )
        }
)
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private String pgTransactionId;

    @Column(nullable = false, unique = true) // 필수값 설정
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private int amount;

    public static Payment ready(Long orderId, int amount, String key) {
        Payment p = new Payment();
        p.orderId = orderId;
        p.amount = amount;
        p.status = PaymentStatus.READY;
        p.idempotencyKey = key;
        return p;
    }

    public void success(String pgTxId) {
        this.status = PaymentStatus.SUCCESS;
        this.pgTransactionId = pgTxId;
    }

    public void fail() {
        this.status = PaymentStatus.FAIL;
    }

    public void unknown() {
        this.status = PaymentStatus.UNKNOWN;
    }
}