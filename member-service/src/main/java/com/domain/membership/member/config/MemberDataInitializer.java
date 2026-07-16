package com.domain.membership.member.config;

import com.domain.membership.common.seed.SeedDataSpec;
import com.domain.membership.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds bulk sample members on startup when the table is empty.
 * The population is defined by {@link SeedDataSpec} (userId 1001-2000) and
 * mirrored by payment-service's seed, which references these members by
 * aggregate id (userId - 1000) — valid when both databases start empty.
 * Rows are inserted via JDBC because the entity builder pins timestamps to
 * now(), while seed members need historical subscribe/expiry dates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberDataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        if (memberRepository.count() > 0) {
            return;
        }

        LocalDate today = LocalDate.now();
        List<Object[]> rows = new ArrayList<>();
        for (long userId = SeedDataSpec.FIRST_USER_ID; userId <= SeedDataSpec.LAST_USER_ID; userId++) {
            SeedDataSpec.MemberSeed seed = SeedDataSpec.of(userId, today);
            LocalDateTime updatedAt = seed.cancelledAt() != null ? seed.cancelledAt() : seed.subscribedAt();
            rows.add(new Object[]{
                    seed.userId(), seed.grade().name(), seed.status().name(),
                    seed.subscribedAt(), seed.expiredAt(), seed.cancelledAt(),
                    seed.subscribedAt(), updatedAt
            });
        }

        jdbcTemplate.batchUpdate("""
                INSERT INTO members
                    (user_id, grade, status, subscribed_at, expired_at, cancelled_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, rows);

        log.info("회원 시드 데이터 등록 완료: {}건", memberRepository.count());
    }
}
