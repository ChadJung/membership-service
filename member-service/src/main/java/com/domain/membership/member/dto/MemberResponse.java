package com.domain.membership.member.dto;

import com.domain.membership.member.entity.Member;
import com.domain.membership.common.type.MembershipGrade;
import com.domain.membership.common.type.MembershipStatus;

import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        Long userId,
        MembershipGrade grade,
        String gradeDisplayName,
        MembershipStatus status,
        LocalDateTime subscribedAt,
        LocalDateTime expiredAt
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getUserId(),
                member.getGrade(),
                member.getGrade().getDisplayName(),
                member.getStatus(),
                member.getSubscribedAt(),
                member.getExpiredAt()
        );
    }
}
