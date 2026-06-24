package com.baemin.membership.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
        @NotNull(message = "사용자 ID는 필수입니다.")
        Long userId,

        @NotBlank(message = "결제 수단은 필수입니다.")
        String paymentMethod
) {
}
