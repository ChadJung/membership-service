package com.domain.membership.common.event;

/**
 * Cross-service event contract published to the payment-events topic.
 * Consumed by member-service: a COMPLETED payment renews the membership,
 * so the event must carry memberId (the member-service aggregate id).
 */
public record PaymentEvent(
        String type,
        Long paymentId,
        Long memberId,
        Long userId,
        int amount
) {
    public static final String TYPE_COMPLETED = "COMPLETED";
    public static final String TYPE_FAILED = "FAILED";

    public static PaymentEvent completed(Long paymentId, Long memberId, Long userId, int amount) {
        return new PaymentEvent(TYPE_COMPLETED, paymentId, memberId, userId, amount);
    }

    public static PaymentEvent failed(Long paymentId, Long memberId, Long userId, int amount) {
        return new PaymentEvent(TYPE_FAILED, paymentId, memberId, userId, amount);
    }
}
