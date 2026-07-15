package com.domain.membership.common.client;

import com.domain.membership.common.type.MembershipGrade;
import com.domain.membership.common.type.MembershipStatus;

import java.time.LocalDateTime;

/**
 * Internal API contract returned by member-service
 * (GET /internal/members/{userId} and /internal/members/{userId}/active).
 * The monthly fee is derived from the grade enum on the consumer side so the
 * amount can never be taken from an external request.
 */
public record MemberSnapshot(
        Long memberId,
        Long userId,
        MembershipGrade grade,
        MembershipStatus status,
        LocalDateTime expiredAt
) {
}
