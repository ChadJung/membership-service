package com.domain.membership.global.config;

import com.domain.membership.domain.benefit.entity.Benefit;
import com.domain.membership.domain.benefit.entity.BenefitType;
import com.domain.membership.domain.benefit.repository.BenefitRepository;
import com.domain.membership.domain.member.entity.MembershipGrade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds benefit master data on startup when the table is empty.
 * Idempotent: skipped entirely once any benefit row exists, so it is safe
 * for both local (create-drop) and prod (update) profiles.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BenefitDataInitializer implements CommandLineRunner {

    private final BenefitRepository benefitRepository;

    @Override
    public void run(String... args) {
        if (benefitRepository.count() > 0) {
            return;
        }

        benefitRepository.saveAll(List.of(
                Benefit.builder()
                        .name("배달비 할인 쿠폰")
                        .description("주문당 배달비 1,000원 할인")
                        .type(BenefitType.DISCOUNT_COUPON)
                        .requiredGrade(MembershipGrade.BASIC)
                        .discountValue(1000)
                        .build(),
                Benefit.builder()
                        .name("포인트 적립")
                        .description("주문 금액의 0.5% 포인트 적립")
                        .type(BenefitType.POINT_REWARD)
                        .requiredGrade(MembershipGrade.BASIC)
                        .discountValue(500)
                        .build(),
                Benefit.builder()
                        .name("무료배달")
                        .description("모든 주문 배달비 무료")
                        .type(BenefitType.FREE_DELIVERY)
                        .requiredGrade(MembershipGrade.PREMIUM)
                        .discountValue(3000)
                        .build(),
                Benefit.builder()
                        .name("프리미엄 할인 쿠폰")
                        .description("주문당 2,000원 할인")
                        .type(BenefitType.DISCOUNT_COUPON)
                        .requiredGrade(MembershipGrade.PREMIUM)
                        .discountValue(2000)
                        .build(),
                Benefit.builder()
                        .name("단독 메뉴 접근")
                        .description("프리미엄 회원 전용 메뉴 주문 가능")
                        .type(BenefitType.EXCLUSIVE_MENU)
                        .requiredGrade(MembershipGrade.PREMIUM)
                        .discountValue(0)
                        .build()
        ));

        log.info("혜택 시드 데이터 등록 완료: {}건", benefitRepository.count());
    }
}
