package com.domain.membership.benefit.service;

import com.domain.membership.benefit.client.MemberClient;
import com.domain.membership.benefit.dto.BenefitResponse;
import com.domain.membership.benefit.entity.Benefit;
import com.domain.membership.benefit.entity.BenefitType;
import com.domain.membership.benefit.repository.BenefitRepository;
import com.domain.membership.common.client.MemberSnapshot;
import com.domain.membership.common.exception.BusinessException;
import com.domain.membership.common.exception.ErrorCode;
import com.domain.membership.common.type.MembershipGrade;
import com.domain.membership.common.type.MembershipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BenefitServiceTest {

    @Mock
    private BenefitRepository benefitRepository;

    @Mock
    private MemberClient memberClient;

    private BenefitService benefitService;

    @BeforeEach
    void setUp() {
        benefitService = new BenefitService(memberClient, new BenefitReader(benefitRepository));
    }

    @Test
    @DisplayName("활성 멤버십 등급에 맞는 혜택 목록 조회 성공")
    void getAvailableBenefits_success() {
        // given
        MemberSnapshot member = new MemberSnapshot(1L, 1001L, MembershipGrade.PREMIUM,
                MembershipStatus.ACTIVE, LocalDateTime.now().plusMonths(1));
        Benefit benefit = Benefit.builder()
                .name("무료배달")
                .description("프리미엄 무료배달")
                .type(BenefitType.FREE_DELIVERY)
                .requiredGrade(MembershipGrade.PREMIUM)
                .discountValue(3000)
                .build();
        given(memberClient.getActiveMember(1001L)).willReturn(member);
        given(benefitRepository.findByRequiredGradeAndActiveTrue(MembershipGrade.PREMIUM))
                .willReturn(List.of(benefit));

        // when
        List<BenefitResponse> benefits = benefitService.getAvailableBenefits(1001L);

        // then
        assertThat(benefits).hasSize(1);
        assertThat(benefits.get(0).name()).isEqualTo("무료배달");
        assertThat(benefits.get(0).type()).isEqualTo(BenefitType.FREE_DELIVERY);
    }

    @Test
    @DisplayName("활성 멤버십이 없으면 혜택 조회 실패")
    void getAvailableBenefits_membershipNotFound() {
        // given
        given(memberClient.getActiveMember(1001L))
                .willThrow(new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> benefitService.getAvailableBenefits(1001L))
                .isInstanceOf(BusinessException.class);
    }
}
