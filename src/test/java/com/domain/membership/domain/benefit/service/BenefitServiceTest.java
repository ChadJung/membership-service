package com.domain.membership.domain.benefit.service;

import com.domain.membership.domain.benefit.dto.BenefitResponse;
import com.domain.membership.domain.benefit.entity.Benefit;
import com.domain.membership.domain.benefit.entity.BenefitType;
import com.domain.membership.domain.benefit.repository.BenefitRepository;
import com.domain.membership.domain.member.entity.Member;
import com.domain.membership.domain.member.entity.MembershipGrade;
import com.domain.membership.domain.member.entity.MembershipStatus;
import com.domain.membership.domain.member.repository.MemberRepository;
import com.domain.membership.domain.member.service.MemberReader;
import com.domain.membership.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BenefitServiceTest {

    @Mock
    private BenefitRepository benefitRepository;

    @Mock
    private MemberRepository memberRepository;

    private BenefitService benefitService;

    @BeforeEach
    void setUp() {
        benefitService = new BenefitService(new MemberReader(memberRepository), new BenefitReader(benefitRepository));
    }

    @Test
    @DisplayName("활성 멤버십 등급에 맞는 혜택 목록 조회 성공")
    void getAvailableBenefits_success() {
        // given
        Member member = Member.builder().userId(1L).grade(MembershipGrade.PREMIUM).build();
        Benefit benefit = Benefit.builder()
                .name("무료배달")
                .description("프리미엄 무료배달")
                .type(BenefitType.FREE_DELIVERY)
                .requiredGrade(MembershipGrade.PREMIUM)
                .discountValue(3000)
                .build();
        given(memberRepository.findByUserIdAndStatus(1L, MembershipStatus.ACTIVE))
                .willReturn(Optional.of(member));
        given(benefitRepository.findByRequiredGradeAndActiveTrue(MembershipGrade.PREMIUM))
                .willReturn(List.of(benefit));

        // when
        List<BenefitResponse> benefits = benefitService.getAvailableBenefits(1L);

        // then
        assertThat(benefits).hasSize(1);
        assertThat(benefits.get(0).name()).isEqualTo("무료배달");
        assertThat(benefits.get(0).type()).isEqualTo(BenefitType.FREE_DELIVERY);
    }

    @Test
    @DisplayName("활성 멤버십이 없으면 혜택 조회 실패")
    void getAvailableBenefits_membershipNotFound() {
        // given
        given(memberRepository.findByUserIdAndStatus(1L, MembershipStatus.ACTIVE))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> benefitService.getAvailableBenefits(1L))
                .isInstanceOf(BusinessException.class);
    }
}
