package com.domain.membership.common.event;

/**
 * Cross-service event contract published to the membership-events topic.
 * Consumed by benefit-service to invalidate its member snapshot cache.
 */
public record MembershipEvent(
        String type,
        Long userId,
        String grade
) {
    public static final String TYPE_SUBSCRIBED = "SUBSCRIBED";
    public static final String TYPE_CANCELLED = "CANCELLED";

    public static MembershipEvent subscribed(Long userId, String grade) {
        return new MembershipEvent(TYPE_SUBSCRIBED, userId, grade);
    }

    public static MembershipEvent cancelled(Long userId, String grade) {
        return new MembershipEvent(TYPE_CANCELLED, userId, grade);
    }
}
