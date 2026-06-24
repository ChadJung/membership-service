package com.baemin.membership.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MembershipGrade {
    BASIC("베이직", 2990),
    PREMIUM("프리미엄", 7900);

    private final String displayName;
    private final int monthlyFee;
}
