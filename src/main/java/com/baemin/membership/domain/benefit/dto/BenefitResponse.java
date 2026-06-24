package com.baemin.membership.domain.benefit.dto;

import com.baemin.membership.domain.benefit.entity.Benefit;
import com.baemin.membership.domain.benefit.entity.BenefitType;

public record BenefitResponse(
        Long id,
        String name,
        String description,
        BenefitType type,
        int discountValue
) {
    public static BenefitResponse from(Benefit benefit) {
        return new BenefitResponse(
                benefit.getId(),
                benefit.getName(),
                benefit.getDescription(),
                benefit.getType(),
                benefit.getDiscountValue()
        );
    }
}
