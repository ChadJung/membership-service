package com.baemin.membership.domain.payment.service;

import com.baemin.membership.domain.member.entity.Member;
import com.baemin.membership.domain.member.entity.MembershipGrade;
import com.baemin.membership.domain.member.entity.MembershipStatus;
import com.baemin.membership.domain.member.repository.MemberRepository;
import com.baemin.membership.domain.payment.dto.PaymentRequest;
import com.baemin.membership.domain.payment.dto.PaymentResponse;
import com.baemin.membership.domain.payment.entity.Payment;
import com.baemin.membership.domain.payment.entity.PaymentStatus;
import com.baemin.membership.domain.payment.repository.PaymentRepository;
import com.baemin.membership.global.exception.BusinessException;
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
    private MemberRepository memberRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, memberRepository, eventPublisher);
    }

    @Test
    @DisplayName("결제 성공 시 등급 월 요금으로 결제되고 상태가 COMPLETED")
    void processPayment_success() {
        // given
        Member member = Member.builder().userId(1L).grade(MembershipGrade.PREMIUM).build();
        given(memberRepository.findByUserIdAndStatus(1L, MembershipStatus.ACTIVE))
                .willReturn(Optional.of(member));
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        PaymentResponse response = paymentService.processPayment(new PaymentRequest(1L, "CARD"));

        // then
        assertThat(response.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.amount()).isEqualTo(MembershipGrade.PREMIUM.getMonthlyFee());
        assertThat(response.transactionId()).isNotNull();
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("활성 멤버십이 없으면 결제 실패")
    void processPayment_membershipNotFound() {
        // given
        given(memberRepository.findByUserIdAndStatus(1L, MembershipStatus.ACTIVE))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(new PaymentRequest(1L, "CARD")))
                .isInstanceOf(BusinessException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("결제 처리 중 예외 발생 시 FAILED로 저장되고 BusinessException")
    void processPayment_failure() {
        // given
        Member member = Member.builder().userId(1L).grade(MembershipGrade.BASIC).build();
        given(memberRepository.findByUserIdAndStatus(1L, MembershipStatus.ACTIVE))
                .willReturn(Optional.of(member));
        given(paymentRepository.save(any(Payment.class)))
                .willThrow(new RuntimeException("PG 장애"))
                .willAnswer(inv -> inv.getArgument(0));

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(new PaymentRequest(1L, "CARD")))
                .isInstanceOf(BusinessException.class);
        verify(paymentRepository, times(2)).save(any(Payment.class)); // 결제 시도 + 실패 저장
    }

    @Test
    @DisplayName("결제 내역 조회 성공")
    void getPaymentHistory_success() {
        // given
        Member member = Member.builder().userId(1L).grade(MembershipGrade.BASIC).build();
        Payment payment = Payment.builder().memberId(1L).amount(2990).paymentMethod("CARD").build();
        given(memberRepository.findByUserId(1L)).willReturn(Optional.of(member));
        given(paymentRepository.findByMemberIdOrderByPaymentDateDesc(member.getId()))
                .willReturn(List.of(payment));

        // when
        List<PaymentResponse> history = paymentService.getPaymentHistory(1L);

        // then
        assertThat(history).hasSize(1);
        assertThat(history.get(0).amount()).isEqualTo(2990);
    }

    @Test
    @DisplayName("정기결제 대상 건에 대해 갱신 결제가 생성되고 멤버십이 갱신됨")
    void processScheduledRenewals_success() {
        // given
        Payment due = Payment.builder().memberId(1L).amount(2990).paymentMethod("CARD").build();
        Member member = Member.builder().userId(1L).grade(MembershipGrade.BASIC).build();
        member.cancel(); // 해지 상태에서 정기결제로 재활성화되는지 확인
        given(paymentRepository.findPaymentsDueForRenewal(eq(PaymentStatus.COMPLETED), any(LocalDateTime.class)))
                .willReturn(List.of(due));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        // when
        paymentService.processScheduledRenewals();

        // then
        assertThat(member.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("정기결제 중 일부 건이 실패해도 나머지 건은 계속 처리")
    void processScheduledRenewals_partialFailure() {
        // given
        Payment due1 = Payment.builder().memberId(1L).amount(2990).paymentMethod("CARD").build();
        Payment due2 = Payment.builder().memberId(2L).amount(2990).paymentMethod("CARD").build();
        Member member2 = Member.builder().userId(2L).grade(MembershipGrade.BASIC).build();
        given(paymentRepository.findPaymentsDueForRenewal(eq(PaymentStatus.COMPLETED), any(LocalDateTime.class)))
                .willReturn(List.of(due1, due2));
        given(memberRepository.findById(1L)).willReturn(Optional.empty()); // 첫 건 조회 실패
        given(memberRepository.findById(2L)).willReturn(Optional.of(member2));

        // when
        paymentService.processScheduledRenewals();

        // then
        assertThat(member2.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        verify(paymentRepository, times(1)).save(any(Payment.class)); // 성공한 두 번째 건만 저장
    }
}
