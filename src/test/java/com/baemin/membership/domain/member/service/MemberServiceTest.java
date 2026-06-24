package com.baemin.membership.domain.member.service;

import com.baemin.membership.domain.member.dto.MemberSubscribeRequest;
import com.baemin.membership.domain.member.dto.MemberResponse;
import com.baemin.membership.domain.member.entity.Member;
import com.baemin.membership.domain.member.entity.MembershipGrade;
import com.baemin.membership.domain.member.entity.MembershipStatus;
import com.baemin.membership.domain.member.repository.MemberRepository;
import com.baemin.membership.global.exception.BusinessException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(memberRepository, eventPublisher, meterRegistry);
    }

    @Test
    @DisplayName("멤버십 가입 성공")
    void subscribe_success() {
        // given
        MemberSubscribeRequest request = new MemberSubscribeRequest(1L, MembershipGrade.BASIC);
        given(memberRepository.existsByUserIdAndStatus(1L, MembershipStatus.ACTIVE)).willReturn(false);
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        MemberResponse response = memberService.subscribe(request);

        // then
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.grade()).isEqualTo(MembershipGrade.BASIC);
        assertThat(response.status()).isEqualTo(MembershipStatus.ACTIVE);
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("이미 활성 멤버십이 있으면 가입 실패")
    void subscribe_alreadySubscribed() {
        // given
        MemberSubscribeRequest request = new MemberSubscribeRequest(1L, MembershipGrade.BASIC);
        given(memberRepository.existsByUserIdAndStatus(1L, MembershipStatus.ACTIVE)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.subscribe(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("멤버십 해지 성공")
    void cancel_success() {
        // given
        Member member = Member.builder()
                .userId(1L)
                .grade(MembershipGrade.BASIC)
                .build();
        given(memberRepository.findByUserIdAndStatus(1L, MembershipStatus.ACTIVE))
                .willReturn(Optional.of(member));

        // when
        MemberResponse response = memberService.cancel(1L);

        // then
        assertThat(response.status()).isEqualTo(MembershipStatus.CANCELLED);
    }

    @Test
    @DisplayName("멤버십 조회 성공")
    void getMembership_success() {
        // given
        Member member = Member.builder()
                .userId(1L)
                .grade(MembershipGrade.PREMIUM)
                .build();
        given(memberRepository.findByUserId(1L)).willReturn(Optional.of(member));

        // when
        MemberResponse response = memberService.getMembership(1L);

        // then
        assertThat(response.grade()).isEqualTo(MembershipGrade.PREMIUM);
        assertThat(response.gradeDisplayName()).isEqualTo("프리미엄");
    }
}
