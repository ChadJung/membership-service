package com.baemin.membership.domain.member.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MemberTest {

    @Test
    @DisplayName("멤버 생성 시 초기 상태가 ACTIVE이고 만료일이 한 달 뒤")
    void create_member() {
        Member member = Member.builder()
                .userId(1L)
                .grade(MembershipGrade.BASIC)
                .build();

        assertThat(member.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(member.getExpiredAt()).isAfter(member.getSubscribedAt());
    }

    @Test
    @DisplayName("활성 멤버십 해지 성공")
    void cancel_active_membership() {
        Member member = Member.builder()
                .userId(1L)
                .grade(MembershipGrade.BASIC)
                .build();

        member.cancel();

        assertThat(member.getStatus()).isEqualTo(MembershipStatus.CANCELLED);
        assertThat(member.getCancelledAt()).isNotNull();
    }

    @Test
    @DisplayName("비활성 멤버십 해지 시 예외")
    void cancel_inactive_membership_throws() {
        Member member = Member.builder()
                .userId(1L)
                .grade(MembershipGrade.BASIC)
                .build();
        member.cancel();

        assertThatThrownBy(member::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("활성 상태의 멤버십만 해지할 수 있습니다.");
    }

    @Test
    @DisplayName("동일 등급으로 변경 시 예외")
    void upgrade_same_grade_throws() {
        Member member = Member.builder()
                .userId(1L)
                .grade(MembershipGrade.BASIC)
                .build();

        assertThatThrownBy(() -> member.upgradeGrade(MembershipGrade.BASIC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("멤버십 갱신 시 상태와 만료일 갱신")
    void renew_membership() {
        Member member = Member.builder()
                .userId(1L)
                .grade(MembershipGrade.BASIC)
                .build();
        member.cancel();
        member.renew();

        assertThat(member.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(member.getCancelledAt()).isNull();
    }
}
