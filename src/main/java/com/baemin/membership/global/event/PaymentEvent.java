package com.baemin.membership.global.event;

import com.baemin.membership.domain.payment.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentEvent {

    private final String type;
    private final Long paymentId;
    private final int amount;

    public static PaymentEvent completed(Payment payment) {
        return new PaymentEvent("COMPLETED", payment.getId(), payment.getAmount());
    }

    public static PaymentEvent failed(Payment payment) {
        return new PaymentEvent("FAILED", payment.getId(), payment.getAmount());
    }
}
