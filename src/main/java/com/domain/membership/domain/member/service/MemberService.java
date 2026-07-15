package com.domain.membership.domain.member.service;

import com.domain.membership.domain.member.dto.MemberSubscribeRequest;
import com.domain.membership.domain.member.dto.MemberResponse;
import com.domain.membership.domain.member.entity.Member;
import com.domain.membership.domain.member.entity.MembershipStatus;
import com.domain.membership.domain.member.repository.MemberRepository;
import com.domain.membership.global.event.MembershipEvent;
import com.domain.membership.global.exception.BusinessException;
import com.domain.membership.global.exception.ErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    @Transactional
    @CacheEvict(value = "members", key = "#request.userId()")
    public MemberResponse subscribe(MemberSubscribeRequest request) {
        if (memberRepository.existsByUserIdAndStatus(request.userId(), MembershipStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.ALREADY_SUBSCRIBED);
        }

        Member member = Member.builder()
                .userId(request.userId())
                .grade(request.grade())
                .build();

        memberRepository.save(member);

        Counter.builder("membership.subscribe")
                .tag("grade", member.getGrade().name())
                .register(meterRegistry)
                .increment();

        eventPublisher.publishEvent(MembershipEvent.subscribed(member));
        log.info("멤버십 가입 완료: userId={}, grade={}", member.getUserId(), member.getGrade());

        return MemberResponse.from(member);
    }

    @Transactional
    @CacheEvict(value = "members", key = "#userId")
    public MemberResponse cancel(Long userId) {
        Member member = memberRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));

        member.cancel();

        Counter.builder("membership.cancel")
                .tag("grade", member.getGrade().name())
                .register(meterRegistry)
                .increment();

        eventPublisher.publishEvent(MembershipEvent.cancelled(member));
        log.info("멤버십 해지 완료: userId={}", userId);

        return MemberResponse.from(member);
    }

    public MemberResponse getMembership(Long userId) {
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));
        return MemberResponse.from(member);
    }
}
