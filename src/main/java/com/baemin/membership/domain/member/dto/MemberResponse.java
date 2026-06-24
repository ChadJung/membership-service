package com.baemin.membership.domain.member.dto;

import com.baemin.membership.domain.member.entity.Member;
import com.baemin.membership.domain.member.entity.MembershipGrade;
import com.baemin.membership.domain.member.entity.MembershipStatus;

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
