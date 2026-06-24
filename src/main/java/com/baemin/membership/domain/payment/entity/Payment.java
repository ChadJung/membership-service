package com.baemin.membership.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false, length = 50)
    private String paymentMethod;

    @Column(unique = true, length = 100)
    private String transactionId;

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    private LocalDateTime nextPaymentDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Payment(Long memberId, int amount, String paymentMethod) {
        this.memberId = memberId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = PaymentStatus.PENDING;
        this.paymentDate = LocalDateTime.now();
        this.nextPaymentDate = LocalDateTime.now().plusMonths(1);
        this.createdAt = LocalDateTime.now();
    }

    public void complete(String transactionId) {
        this.status = PaymentStatus.COMPLETED;
        this.transactionId = transactionId;
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
    }

    public void refund() {
        if (this.status != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("완료된 결제만 환불할 수 있습니다.");
        }
        this.status = PaymentStatus.REFUNDED;
    }
}
