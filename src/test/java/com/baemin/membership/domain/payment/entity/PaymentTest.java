package com.baemin.membership.domain.payment.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PaymentTest {

    private Payment createPayment() {
        return Payment.builder()
                .memberId(1L)
                .amount(2990)
                .paymentMethod("CARD")
                .build();
    }

    @Test
    @DisplayName("결제 생성 시 초기 상태는 PENDING이고 다음 결제일은 한 달 뒤")
    void create_payment() {
        Payment payment = createPayment();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getNextPaymentDate()).isAfter(payment.getPaymentDate());
    }

    @Test
    @DisplayName("결제 완료 시 상태 COMPLETED 및 거래 ID 설정")
    void complete_payment() {
        Payment payment = createPayment();

        payment.complete("tx-123");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getTransactionId()).isEqualTo("tx-123");
    }

    @Test
    @DisplayName("결제 실패 시 상태 FAILED")
    void fail_payment() {
        Payment payment = createPayment();

        payment.fail();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("완료된 결제는 환불 가능")
    void refund_completed_payment() {
        Payment payment = createPayment();
        payment.complete("tx-123");

        payment.refund();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("완료되지 않은 결제 환불 시 예외")
    void refund_uncompleted_payment_throws() {
        Payment payment = createPayment();

        assertThatThrownBy(payment::refund)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("완료된 결제만 환불할 수 있습니다.");
    }
}
