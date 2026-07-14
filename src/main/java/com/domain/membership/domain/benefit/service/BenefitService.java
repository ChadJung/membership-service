package com.domain.membership.domain.benefit.service;

import com.domain.membership.domain.benefit.dto.BenefitResponse;
import com.domain.membership.domain.member.entity.Member;
import com.domain.membership.domain.member.entity.MembershipStatus;
import com.domain.membership.domain.member.repository.MemberRepository;
import com.domain.membership.global.exception.BusinessException;
import com.domain.membership.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BenefitService {

    private final MemberRepository memberRepository;
    private final BenefitReader benefitReader;

    public List<BenefitResponse> getAvailableBenefits(Long userId) {
        // Membership validation stays outside the cache so ACTIVE status and
        // the member's current grade are always checked against the DB.
        Member member = memberRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));

        return benefitReader.getActiveBenefits(member.getGrade());
    }
}
