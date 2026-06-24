package com.baemin.membership.domain.benefit.controller;

import com.baemin.membership.domain.benefit.dto.BenefitResponse;
import com.baemin.membership.domain.benefit.service.BenefitService;
import com.baemin.membership.global.response.ApiResponse;
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
