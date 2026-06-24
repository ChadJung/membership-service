package com.baemin.membership.domain.benefit.service;

import com.baemin.membership.domain.benefit.dto.BenefitResponse;
import com.baemin.membership.domain.benefit.repository.BenefitRepository;
import com.baemin.membership.domain.member.entity.Member;
import com.baemin.membership.domain.member.entity.MembershipStatus;
import com.baemin.membership.domain.member.repository.MemberRepository;
import com.baemin.membership.global.exception.BusinessException;
import com.baemin.membership.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BenefitService {

    private final BenefitRepository benefitRepository;
    private final MemberRepository memberRepository;

    @Cacheable(value = "benefits", key = "#userId")
    public List<BenefitResponse> getAvailableBenefits(Long userId) {
        Member member = memberRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));

        return benefitRepository.findByRequiredGradeAndActiveTrue(member.getGrade())
                .stream()
                .map(BenefitResponse::from)
                .toList();
    }
}
