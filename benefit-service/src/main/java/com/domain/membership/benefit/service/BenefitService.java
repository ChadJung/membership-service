package com.domain.membership.benefit.service;

import com.domain.membership.benefit.client.MemberClient;
import com.domain.membership.benefit.dto.BenefitResponse;
import com.domain.membership.common.client.MemberSnapshot;
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

    private final MemberClient memberClient;
    private final BenefitReader benefitReader;

    public List<BenefitResponse> getAvailableBenefits(Long userId) {
        // Both lookups are cached: the membership snapshot briefly (60s TTL,
        // evicted via membership-events) and the grade's benefit list longer.
        MemberSnapshot member = memberClient.getActiveMember(userId);
        return benefitReader.getActiveBenefits(member.grade());
    }
}
