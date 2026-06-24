package com.baemin.membership.domain.payment.service;

import com.baemin.membership.domain.member.entity.Member;
import com.baemin.membership.domain.member.entity.MembershipStatus;
import com.baemin.membership.domain.member.repository.MemberRepository;
import com.baemin.membership.domain.payment.dto.PaymentRequest;
import com.baemin.membership.domain.payment.dto.PaymentResponse;
import com.baemin.membership.domain.payment.entity.Payment;
import com.baemin.membership.domain.payment.entity.PaymentStatus;
import com.baemin.membership.domain.payment.repository.PaymentRepository;
import com.baemin.membership.global.event.PaymentEvent;
import com.baemin.membership.global.exception.BusinessException;
import com.baemin.membership.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        Member member = memberRepository.findByUserIdAndStatus(request.userId(), MembershipStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));

        Payment payment = Payment.builder()
                .memberId(member.getId())
                .amount(member.getGrade().getMonthlyFee())
                .paymentMethod(request.paymentMethod())
                .build();

        try {
            String transactionId = UUID.randomUUID().toString();
            payment.complete(transactionId);
            paymentRepository.save(payment);

            member.renew();

            eventPublisher.publishEvent(PaymentEvent.completed(payment));
            log.info("결제 완료: memberId={}, amount={}", member.getId(), payment.getAmount());
        } catch (Exception e) {
            payment.fail();
            paymentRepository.save(payment);
            eventPublisher.publishEvent(PaymentEvent.failed(payment));
            log.error("결제 실패: memberId={}", member.getId(), e);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }

        return PaymentResponse.from(payment);
    }

    public List<PaymentResponse> getPaymentHistory(Long userId) {
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));

        return paymentRepository.findByMemberIdOrderByPaymentDateDesc(member.getId())
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional
    public void processScheduledRenewals() {
        List<Payment> duePayments = paymentRepository.findPaymentsDueForRenewal(
                PaymentStatus.COMPLETED, java.time.LocalDateTime.now());

        log.info("정기결제 대상: {}건", duePayments.size());

        for (Payment lastPayment : duePayments) {
            try {
                Member member = memberRepository.findById(lastPayment.getMemberId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));

                Payment renewal = Payment.builder()
                        .memberId(member.getId())
                        .amount(member.getGrade().getMonthlyFee())
                        .paymentMethod(lastPayment.getPaymentMethod())
                        .build();

                String transactionId = UUID.randomUUID().toString();
                renewal.complete(transactionId);
                paymentRepository.save(renewal);

                member.renew();
                log.info("정기결제 갱신 완료: memberId={}", member.getId());
            } catch (Exception e) {
                log.error("정기결제 갱신 실패: memberId={}", lastPayment.getMemberId(), e);
            }
        }
    }
}
