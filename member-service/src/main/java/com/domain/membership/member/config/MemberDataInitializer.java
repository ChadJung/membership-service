package com.domain.membership.member.config;

import com.domain.membership.common.type.MembershipGrade;
import com.domain.membership.member.entity.Member;
import com.domain.membership.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds sample members on startup when the table is empty.
 * userId 1001-1010: odd ids BASIC, even ids PREMIUM; 1009/1010 cancelled.
 * payment-service's seed references these members by aggregate id (1-10),
 * which holds when both services start from empty databases.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberDataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;

    @Override
    public void run(String... args) {
        if (memberRepository.count() > 0) {
            return;
        }

        List<Member> members = new ArrayList<>();
        for (long userId = 1001; userId <= 1010; userId++) {
            Member member = Member.builder()
                    .userId(userId)
                    .grade(userId % 2 == 1 ? MembershipGrade.BASIC : MembershipGrade.PREMIUM)
                    .build();
            if (userId >= 1009) {
                member.cancel();
            }
            members.add(member);
        }
        memberRepository.saveAll(members);

        log.info("회원 시드 데이터 등록 완료: {}건", memberRepository.count());
    }
}
