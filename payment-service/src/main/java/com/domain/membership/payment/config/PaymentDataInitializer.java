package com.domain.membership.payment.config;

import com.domain.membership.common.type.MembershipGrade;
import com.domain.membership.payment.entity.Payment;
import com.domain.membership.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Seeds sample payment history on startup when the table is empty.
 * memberId 1-8 matches member-service's seed (userId 1001-1008, ACTIVE)
 * when both services start from empty databases.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentDataInitializer implements CommandLineRunner {

    private final PaymentRepository paymentRepository;

    @Override
    public void run(String... args) {
        if (paymentRepository.count() > 0) {
            return;
        }

        List<Payment> payments = new ArrayList<>();
        for (long memberId = 1; memberId <= 8; memberId++) {
            MembershipGrade grade = memberId % 2 == 1 ? MembershipGrade.BASIC : MembershipGrade.PREMIUM;
            Payment payment = Payment.builder()
                    .memberId(memberId)
                    .amount(grade.getMonthlyFee())
                    .paymentMethod(memberId % 3 == 0 ? "KAKAO_PAY" : "CARD")
                    .build();
            payment.complete(UUID.randomUUID().toString());
            payments.add(payment);
        }
        paymentRepository.saveAll(payments);

        log.info("결제 시드 데이터 등록 완료: {}건", paymentRepository.count());
    }
}
