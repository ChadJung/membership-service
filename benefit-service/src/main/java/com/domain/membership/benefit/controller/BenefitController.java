package com.domain.membership.benefit.controller;

import com.domain.membership.benefit.dto.BenefitResponse;
import com.domain.membership.benefit.service.BenefitService;
import com.domain.membership.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/benefits")
@RequiredArgsConstructor
public class BenefitController {

    private final BenefitService benefitService;

    @GetMapping("/{userId}")
    public ApiResponse<List<BenefitResponse>> getAvailableBenefits(@PathVariable Long userId) {
        return ApiResponse.success(benefitService.getAvailableBenefits(userId));
    }
}
