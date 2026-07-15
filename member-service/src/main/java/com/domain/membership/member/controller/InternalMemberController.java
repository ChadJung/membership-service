package com.domain.membership.member.controller;

import com.domain.membership.common.client.MemberSnapshot;
import com.domain.membership.common.exception.BusinessException;
import com.domain.membership.common.exception.ErrorCode;
import com.domain.membership.common.type.MembershipStatus;
import com.domain.membership.member.entity.Member;
import com.domain.membership.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service API (payment-service, benefit-service).
 * Not routed through the API gateway — internal network only.
 */
@RestController
@RequestMapping("/internal/members")
@RequiredArgsConstructor
public class InternalMemberController {

    private final MemberRepository memberRepository;

    @GetMapping("/{userId}")
    public MemberSnapshot getMember(@PathVariable Long userId) {
        return memberRepository.findByUserId(userId)
                .map(this::toSnapshot)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));
    }

    // Lookup by aggregate id, used by payment-service's renewal scheduler
    // (payments reference members by memberId, not userId).
    @GetMapping("/by-id/{memberId}")
    public MemberSnapshot getMemberById(@PathVariable Long memberId) {
        return memberRepository.findById(memberId)
                .map(this::toSnapshot)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));
    }

    @GetMapping("/{userId}/active")
    public MemberSnapshot getActiveMember(@PathVariable Long userId) {
        return memberRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE)
                .map(this::toSnapshot)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));
    }

    private MemberSnapshot toSnapshot(Member member) {
        return new MemberSnapshot(
                member.getId(),
                member.getUserId(),
                member.getGrade(),
                member.getStatus(),
                member.getExpiredAt()
        );
    }
}
