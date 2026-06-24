package com.baemin.membership.domain.payment.controller;

import com.baemin.membership.domain.payment.dto.PaymentRequest;
import com.baemin.membership.domain.payment.dto.PaymentResponse;
import com.baemin.membership.domain.payment.service.PaymentService;
import com.baemin.membership.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PaymentResponse> processPayment(@RequestBody @Valid PaymentRequest request) {
        return ApiResponse.success(paymentService.processPayment(request));
    }

    @GetMapping("/{userId}")
    public ApiResponse<List<PaymentResponse>> getPaymentHistory(@PathVariable Long userId) {
        return ApiResponse.success(paymentService.getPaymentHistory(userId));
    }
}
