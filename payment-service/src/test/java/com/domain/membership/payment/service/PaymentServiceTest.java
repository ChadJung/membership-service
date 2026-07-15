package com.domain.membership.payment.service;

import com.domain.membership.common.client.MemberSnapshot;
import com.domain.membership.common.event.PaymentEvent;
import com.domain.membership.common.exception.BusinessException;
import com.domain.membership.common.exception.ErrorCode;
import com.domain.membership.common.type.MembershipGrade;
import com.domain.membership.common.type.MembershipStatus;
import com.domain.membership.payment.client.MemberClient;
import com.domain.membership.payment.dto.PaymentRequest;
import com.domain.membership.payment.dto.PaymentResponse;
import com.domain.membership.payment.entity.Payment;
import com.domain.membership.payment.entity.PaymentStatus;
import com.domain.membership.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MemberClient memberClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, memberClient, eventPublisher);
    }

    private MemberSnapshot snapshot(Long memberId, Long userId, MembershipGrade grade) {
        return new MemberSnapshot(memberId, userId, grade, MembershipStatus.ACTIVE,
                LocalDateTime.now().plusMonths(1));
    }

    @Test
    @DisplayName("결제 성공 시 등급 월 요금으로 결제되고 상태가 COMPLETED")
    void processPayment_success() {
        // given
        given(memberClient.getActiveMember(1001L))
                .willReturn(snapshot(1L, 1001L, MembershipGrade.PREMIUM));
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        PaymentResponse response = paymentService.processPayment(new PaymentRequest(1001L, "CARD", null));

        // then
        assertThat(response.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.amount()).isEqualTo(MembershipGrade.PREMIUM.getMonthlyFee());
        assertThat(response.transactionId()).isNotNull();
        verify(paymentRepository).save(any(Payment.class));
        verify(eventPublisher).publishEvent(any(PaymentEvent.class));
    }

    @Test
    @DisplayName("활성 멤버십이 없으면 결제 실패")
    void processPayment_membershipNotFound() {
        // given
        given(memberClient.getActiveMember(1001L))
                .willThrow(new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(new PaymentRequest(1001L, "CARD", null)))
                .isInstanceOf(BusinessException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("결제 처리 중 예외 발생 시 FAILED로 저장되고 BusinessException")
    void processPayment_failure() {
        // given
        given(memberClient.getActiveMember(1001L))
                .willReturn(snapshot(1L, 1001L, MembershipGrade.BASIC));
        given(paymentRepository.save(any(Payment.class)))
                .willThrow(new RuntimeException("PG 장애"))
                .willAnswer(inv -> inv.getArgument(0));

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(new PaymentRequest(1001L, "CARD", null)))
                .isInstanceOf(BusinessException.class);
        verify(paymentRepository, times(2)).save(any(Payment.class)); // 결제 시도 + 실패 저장
    }

    @Test
    @DisplayName("현재 결제 주기에 완료된 결제가 있으면 중복 결제 거부")
    void processPayment_alreadyPaidThisCycle() {
        // given
        given(memberClient.getActiveMember(1001L))
                .willReturn(snapshot(1L, 1001L, MembershipGrade.BASIC));
        given(paymentRepository.existsByMemberIdAndStatusAndNextPaymentDateAfter(
                eq(1L), eq(PaymentStatus.COMPLETED), any(LocalDateTime.class)))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(new PaymentRequest(1001L, "CARD", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_ALREADY_PAID);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("동일 멱등성 키 재요청 시 기존 결제를 반환하고 재과금하지 않음")
    void processPayment_idempotentReplay() {
        // given
        Payment existing = Payment.builder()
                .memberId(1L).amount(2990).paymentMethod("CARD")
                .idempotencyKey("key-123").build();
        existing.complete("tx-1");
        given(paymentRepository.findByIdempotencyKey("key-123")).willReturn(Optional.of(existing));

        // when
        PaymentResponse response = paymentService.processPayment(
                new PaymentRequest(1001L, "CARD", "key-123"));

        // then
        assertThat(response.transactionId()).isEqualTo("tx-1");
        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("결제 내역 조회 성공")
    void getPaymentHistory_success() {
        // given
        Payment payment = Payment.builder().memberId(1L).amount(2990).paymentMethod("CARD").build();
        given(memberClient.getMember(1001L)).willReturn(snapshot(1L, 1001L, MembershipGrade.BASIC));
        given(paymentRepository.findByMemberIdOrderByPaymentDateDesc(1L))
                .willReturn(List.of(payment));

        // when
        List<PaymentResponse> history = paymentService.getPaymentHistory(1001L);

        // then
        assertThat(history).hasSize(1);
        assertThat(history.get(0).amount()).isEqualTo(2990);
    }

    @Test
    @DisplayName("정기결제 대상 건에 갱신 결제가 생성되고 결제 완료 이벤트가 발행됨")
    void processScheduledRenewals_success() {
        // given
        Payment due = Payment.builder().memberId(1L).amount(2990).paymentMethod("CARD").build();
        given(paymentRepository.findPaymentsDueForRenewal(eq(PaymentStatus.COMPLETED), any(LocalDateTime.class)))
                .willReturn(List.of(due));
        given(memberClient.getMemberById(1L)).willReturn(snapshot(1L, 1001L, MembershipGrade.BASIC));
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        paymentService.processScheduledRenewals();

        // then: 갱신 자체는 member-service가 이벤트를 소비해서 수행한다
        verify(paymentRepository).save(any(Payment.class));
        verify(eventPublisher).publishEvent(any(PaymentEvent.class));
    }

    @Test
    @DisplayName("정기결제 중 일부 건이 실패해도 나머지 건은 계속 처리")
    void processScheduledRenewals_partialFailure() {
        // given
        Payment due1 = Payment.builder().memberId(1L).amount(2990).paymentMethod("CARD").build();
        Payment due2 = Payment.builder().memberId(2L).amount(2990).paymentMethod("CARD").build();
        given(paymentRepository.findPaymentsDueForRenewal(eq(PaymentStatus.COMPLETED), any(LocalDateTime.class)))
                .willReturn(List.of(due1, due2));
        given(memberClient.getMemberById(1L))
                .willThrow(new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND)); // 첫 건 조회 실패
        given(memberClient.getMemberById(2L)).willReturn(snapshot(2L, 1002L, MembershipGrade.PREMIUM));
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        paymentService.processScheduledRenewals();

        // then
        verify(paymentRepository, times(1)).save(any(Payment.class)); // 성공한 두 번째 건만 저장
        verify(eventPublisher, times(1)).publishEvent(any(PaymentEvent.class));
    }
}
