package com.domain.membership.benefit.config;

import com.domain.membership.benefit.entity.Benefit;
import com.domain.membership.benefit.entity.BenefitType;
import com.domain.membership.benefit.repository.BenefitRepository;
import com.domain.membership.common.type.MembershipGrade;
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
                // BASIC (PREMIUM includes these as well)
                benefit("배달비 할인 쿠폰", "주문당 배달비 1,000원 할인",
                        BenefitType.DISCOUNT_COUPON, MembershipGrade.BASIC, 1000),
                benefit("포인트 적립", "주문 금액의 0.5% 포인트 적립",
                        BenefitType.POINT_REWARD, MembershipGrade.BASIC, 500),
                benefit("첫 주문 할인", "가입 후 첫 주문 3,000원 할인",
                        BenefitType.DISCOUNT_COUPON, MembershipGrade.BASIC, 3000),
                benefit("생일 쿠폰", "생일 주간 2,000원 할인 쿠폰",
                        BenefitType.DISCOUNT_COUPON, MembershipGrade.BASIC, 2000),
                benefit("리뷰 포인트", "포토 리뷰 작성 시 300포인트 적립",
                        BenefitType.POINT_REWARD, MembershipGrade.BASIC, 300),

                // PREMIUM only
                benefit("무료배달", "모든 주문 배달비 무료",
                        BenefitType.FREE_DELIVERY, MembershipGrade.PREMIUM, 3000),
                benefit("프리미엄 할인 쿠폰", "주문당 2,000원 할인",
                        BenefitType.DISCOUNT_COUPON, MembershipGrade.PREMIUM, 2000),
                benefit("단독 메뉴 접근", "프리미엄 회원 전용 메뉴 주문 가능",
                        BenefitType.EXCLUSIVE_MENU, MembershipGrade.PREMIUM, 0),
                benefit("더블 포인트 적립", "주문 금액의 1% 포인트 적립",
                        BenefitType.POINT_REWARD, MembershipGrade.PREMIUM, 1000),
                benefit("주말 브런치 쿠폰", "주말 오전 주문 시 3,000원 할인",
                        BenefitType.DISCOUNT_COUPON, MembershipGrade.PREMIUM, 3000),
                benefit("신메뉴 우선 주문", "신규 입점 매장 메뉴 우선 주문 가능",
                        BenefitType.EXCLUSIVE_MENU, MembershipGrade.PREMIUM, 0),
                benefit("월간 무료배달 부스트", "매월 첫 주 배달비 무료 + 500포인트",
                        BenefitType.FREE_DELIVERY, MembershipGrade.PREMIUM, 500)
        ));

        log.info("혜택 시드 데이터 등록 완료: {}건", benefitRepository.count());
    }

    private Benefit benefit(String name, String description, BenefitType type,
                            MembershipGrade requiredGrade, int discountValue) {
        return Benefit.builder()
                .name(name)
                .description(description)
                .type(type)
                .requiredGrade(requiredGrade)
                .discountValue(discountValue)
                .build();
    }
}
