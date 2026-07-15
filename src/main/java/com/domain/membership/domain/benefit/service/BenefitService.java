package com.domain.membership.domain.benefit.service;

import com.domain.membership.domain.benefit.dto.BenefitResponse;
import com.domain.membership.domain.member.dto.MemberResponse;
import com.domain.membership.domain.member.service.MemberReader;
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

    private final MemberReader memberReader;
    private final BenefitReader benefitReader;

    public List<BenefitResponse> getAvailableBenefits(Long userId) {
        // Both lookups are cached: the membership snapshot briefly (60s TTL,
        // evicted on every state change) and the grade's benefit list longer.
        MemberResponse member = memberReader.getActiveMember(userId);
        return benefitReader.getActiveBenefits(member.grade());
    }
}
