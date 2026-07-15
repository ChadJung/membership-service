package com.domain.membership.domain.member.service;

import com.domain.membership.domain.member.dto.MemberResponse;
import com.domain.membership.domain.member.entity.MembershipStatus;
import com.domain.membership.domain.member.repository.MemberRepository;
import com.domain.membership.global.exception.BusinessException;
import com.domain.membership.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cached ACTIVE-membership snapshot lookup.
 * Separate bean so the cache proxy is not bypassed by self-invocation.
 * Payment authorization intentionally reads the DB directly and must never
 * go through this cache — a stale ACTIVE snapshot could bill a cancelled member.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberReader {

    private final MemberRepository memberRepository;

    // Short-TTL snapshot (60s, see RedisConfig). Every state change goes through
    // MemberService/PaymentService, which evict this key immediately, so the TTL
    // only bounds staleness if an eviction path is ever missed.
    @Cacheable(value = "members", key = "#userId")
    public MemberResponse getActiveMember(Long userId) {
        return memberRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE)
                .map(MemberResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));
    }

    // For callers that mutate members outside an annotated method (e.g. the
    // renewal loop, which evicts per item).
    @CacheEvict(value = "members", key = "#userId")
    public void evictMember(Long userId) {
    }
}
