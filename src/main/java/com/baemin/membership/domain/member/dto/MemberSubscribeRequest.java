package com.baemin.membership.domain.member.dto;

import com.baemin.membership.domain.member.entity.MembershipGrade;
import jakarta.validation.constraints.NotNull;

public record MemberSubscribeRequest(
        @NotNull(message = "사용자 ID는 필수입니다.")
        Long userId,

        @NotNull(message = "멤버십 등급은 필수입니다.")
        MembershipGrade grade
) {
}
