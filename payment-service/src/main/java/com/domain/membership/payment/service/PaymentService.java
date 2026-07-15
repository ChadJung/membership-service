package com.domain.membership.payment.service;

import com.domain.membership.common.client.MemberSnapshot;
import com.domain.membership.common.event.PaymentEvent;
import com.domain.membership.common.exception.BusinessException;
import com.domain.membership.common.exception.ErrorCode;
import com.domain.membership.payment.client.MemberClient;
import com.domain.membership.payment.dto.PaymentRequest;
import com.domain.membership.payment.dto.PaymentResponse;
import com.domain.membership.payment.entity.Payment;
import com.domain.membership.payment.entity.PaymentStatus;
import com.domain.membership.payment.repository.PaymentRepository;
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
    private final MemberClient memberClient;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        // Live authorization against member-service; the fee comes from the
        // grade enum, never from the client request.
        MemberSnapshot member = memberClient.getActiveMember(request.userId());

        Payment payment = Payment.builder()
                .memberId(member.memberId())
                .amount(member.grade().getMonthlyFee())
                .paymentMethod(request.paymentMethod())
                .build();

        try {
            String transactionId = UUID.randomUUID().toString();
            payment.complete(transactionId);
            paymentRepository.save(payment);

            // member-service consumes this event and renews the membership.
            eventPublisher.publishEvent(PaymentEvent.completed(
                    payment.getId(), member.memberId(), member.userId(), payment.getAmount()));
            log.info("결제 완료: memberId={}, amount={}", member.memberId(), payment.getAmount());
        } catch (Exception e) {
            payment.fail();
            paymentRepository.save(payment);
            eventPublisher.publishEvent(PaymentEvent.failed(
                    payment.getId(), member.memberId(), member.userId(), payment.getAmount()));
            log.error("결제 실패: memberId={}", member.memberId(), e);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }

        return PaymentResponse.from(payment);
    }

    public List<PaymentResponse> getPaymentHistory(Long userId) {
        MemberSnapshot member = memberClient.getMember(userId);

        return paymentRepository.findByMemberIdOrderByPaymentDateDesc(member.memberId())
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
                MemberSnapshot member = memberClient.getMemberById(lastPayment.getMemberId());

                Payment renewal = Payment.builder()
                        .memberId(member.memberId())
                        .amount(member.grade().getMonthlyFee())
                        .paymentMethod(lastPayment.getPaymentMethod())
                        .build();

                String transactionId = UUID.randomUUID().toString();
                renewal.complete(transactionId);
                paymentRepository.save(renewal);

                eventPublisher.publishEvent(PaymentEvent.completed(
                        renewal.getId(), member.memberId(), member.userId(), renewal.getAmount()));
                log.info("정기결제 갱신 완료: memberId={}", member.memberId());
            } catch (Exception e) {
                log.error("정기결제 갱신 실패: memberId={}", lastPayment.getMemberId(), e);
            }
        }
    }
}
