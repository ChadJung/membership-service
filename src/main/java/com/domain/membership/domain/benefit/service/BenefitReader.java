package com.domain.membership.domain.benefit.service;

import com.domain.membership.domain.benefit.dto.BenefitResponse;
import com.domain.membership.domain.benefit.repository.BenefitRepository;
import com.domain.membership.domain.member.entity.MembershipGrade;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Grade-keyed cached benefit lookup.
 * Kept as a separate bean so the @Cacheable proxy is not bypassed by
 * self-invocation from BenefitService.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BenefitReader {

    private final BenefitRepository benefitRepository;

    // Benefit lists are grade-scoped master data, so the cache is keyed by grade
    // (one entry per grade) instead of per user. A member's grade change simply
    // resolves to a different key, so no eviction is needed on member updates.
    // The result must be an ArrayList: GenericJackson2JsonRedisSerializer only
    // embeds a type id for non-final classes, and the final immutable list from
    // Stream.toList() round-trips as a bare JSON array that cannot be read back.
    @Cacheable(value = "benefits", key = "#grade", unless = "#result.isEmpty()")
    public List<BenefitResponse> getActiveBenefits(MembershipGrade grade) {
        return benefitRepository.findByRequiredGradeAndActiveTrue(grade)
                .stream()
                .map(BenefitResponse::from)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
