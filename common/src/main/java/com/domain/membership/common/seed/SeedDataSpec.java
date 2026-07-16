package com.domain.membership.common.seed;

import com.domain.membership.common.type.MembershipGrade;
import com.domain.membership.common.type.MembershipStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Deterministic seed-data specification shared by member-service and
 * payment-service so both sides generate a consistent world from empty
 * databases: member aggregate id = userId - 1000 (identity insert order),
 * and each member's payment history / next_payment_date align with the
 * member's subscribed/expired timeline. Every attribute derives from the
 * userId alone — no shared random state, so the services stay consistent
 * without any startup coordination.
 */
public final class SeedDataSpec {

    public static final long FIRST_USER_ID = 1001L;
    public static final long LAST_USER_ID = 2000L;

    private static final String[] PAYMENT_METHODS =
            {"CARD", "KAKAO_PAY", "NAVER_PAY", "BANK_TRANSFER"};

    private SeedDataSpec() {
    }

    public record MemberSeed(
            long userId,
            MembershipGrade grade,
            MembershipStatus status,
            String paymentMethod,
            int paidCycles,
            LocalDateTime subscribedAt,
            LocalDateTime expiredAt,
            LocalDateTime cancelledAt
    ) {
        public long memberId() {
            return userId - (FIRST_USER_ID - 1);
        }
    }

    public static MemberSeed of(long userId, LocalDate today) {
        MembershipGrade grade;
        MembershipStatus status;
        int months;
        if (userId <= 1010) {
            // Keep the originally documented demo pattern for 1001-1010:
            // odd ids BASIC / even ids PREMIUM, 1009-1010 cancelled.
            grade = userId % 2 == 1 ? MembershipGrade.BASIC : MembershipGrade.PREMIUM;
            status = userId >= 1009 ? MembershipStatus.CANCELLED : MembershipStatus.ACTIVE;
            months = 3;
        } else {
            grade = userId % 10 < 6 ? MembershipGrade.BASIC : MembershipGrade.PREMIUM;
            if (userId % 7 == 0) {
                status = MembershipStatus.CANCELLED;
            } else if (userId % 19 == 0) {
                status = MembershipStatus.EXPIRED;
            } else {
                status = MembershipStatus.ACTIVE;
            }
            months = (int) (1 + (userId * 17) % 24);
        }

        int dayOffset = (int) (1 + (userId * 13) % 28);
        int hour = (int) (8 + userId % 12);
        LocalDateTime subscribedAt = today.minusMonths(months).minusDays(dayOffset).atTime(hour, 0);

        // ACTIVE members paid every cycle including the current one, so their
        // expiry lands in the future (spread over the next month, which feeds
        // the renewal scheduler gradually); lapsed members stopped partway.
        int paidCycles = status == MembershipStatus.ACTIVE ? months + 1 : Math.max(1, months / 2);
        LocalDateTime expiredAt = subscribedAt.plusMonths(paidCycles);
        LocalDateTime cancelledAt = status == MembershipStatus.CANCELLED ? expiredAt.minusDays(5) : null;

        return new MemberSeed(userId, grade, status, PAYMENT_METHODS[(int) (userId % 4)],
                paidCycles, subscribedAt, expiredAt, cancelledAt);
    }
}
