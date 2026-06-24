package com.baemin.membership.domain.member.controller;

import com.baemin.membership.domain.member.dto.MemberSubscribeRequest;
import com.baemin.membership.domain.member.dto.MemberResponse;
import com.baemin.membership.domain.member.service.MemberService;
import com.baemin.membership.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/memberships")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberResponse> subscribe(@RequestBody @Valid MemberSubscribeRequest request) {
        return ApiResponse.success(memberService.subscribe(request));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<MemberResponse> cancel(@PathVariable Long userId) {
        return ApiResponse.success(memberService.cancel(userId));
    }

    @GetMapping("/{userId}")
    public ApiResponse<MemberResponse> getMembership(@PathVariable Long userId) {
        return ApiResponse.success(memberService.getMembership(userId));
    }
}
