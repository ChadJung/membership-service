package com.baemin.membership.domain.payment.dto;

import com.baemin.membership.domain.payment.entity.Payment;
import com.baemin.membership.domain.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long memberId,
        int amount,
        PaymentStatus status,
        String paymentMethod,
        String transactionId,
        LocalDateTime paymentDate,
        LocalDateTime nextPaymentDate
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getMemberId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getTransactionId(),
                payment.getPaymentDate(),
                payment.getNextPaymentDate()
        );
    }
}
