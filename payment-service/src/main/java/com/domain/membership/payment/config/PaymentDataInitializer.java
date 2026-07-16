package com.domain.membership.payment.config;

import com.domain.membership.common.seed.SeedDataSpec;
import com.domain.membership.common.type.MembershipStatus;
import com.domain.membership.payment.entity.PaymentStatus;
import com.domain.membership.payment.repository.PaymentRepository;
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
 * Seeds bulk payment history on startup when the table is empty.
 * Rebuilds each member's monthly charge chain from {@link SeedDataSpec}
 * (memberId = userId - 1000, matching member-service's seed on empty DBs).
 * Inserted via JDBC because the entity builder pins paymentDate to now(),
 * while history rows need past billing dates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentDataInitializer implements CommandLineRunner {

    private final PaymentRepository paymentRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final String INSERT_SQL = """
            INSERT INTO payments
                (member_id, amount, status, payment_method, transaction_id,
                 idempotency_key, payment_date, next_payment_date, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    @Override
    public void run(String... args) {
        if (paymentRepository.count() > 0) {
            return;
        }

        LocalDate today = LocalDate.now();
        List<Object[]> rows = new ArrayList<>();
        for (long userId = SeedDataSpec.FIRST_USER_ID; userId <= SeedDataSpec.LAST_USER_ID; userId++) {
            SeedDataSpec.MemberSeed seed = SeedDataSpec.of(userId, today);
            long memberId = seed.memberId();
            int amount = seed.grade().getMonthlyFee();
            boolean active = seed.status() == MembershipStatus.ACTIVE;

            for (int cycle = 0; cycle < seed.paidCycles(); cycle++) {
                LocalDateTime paymentDate = seed.subscribedAt().plusMonths(cycle);
                boolean latest = cycle == seed.paidCycles() - 1;
                // Deschedule invariant: only the latest payment of an ACTIVE
                // member carries next_payment_date (= the member's expiry).
                LocalDateTime nextPaymentDate = (active && latest) ? seed.expiredAt() : null;
                rows.add(new Object[]{
                        memberId, amount, PaymentStatus.COMPLETED.name(), seed.paymentMethod(),
                        "seed-tx-%d-%d".formatted(memberId, cycle), null,
                        paymentDate, nextPaymentDate, paymentDate
                });
            }

            // Sprinkle a failed attempt shortly before the latest charge so
            // history views include non-happy-path rows.
            if (active && userId % 11 == 0) {
                LocalDateTime failedAt = seed.subscribedAt()
                        .plusMonths(seed.paidCycles() - 1).minusHours(3);
                rows.add(new Object[]{
                        memberId, amount, PaymentStatus.FAILED.name(), seed.paymentMethod(),
                        null, null, failedAt, null, failedAt
                });
            }
        }

        jdbcTemplate.batchUpdate(INSERT_SQL, rows);

        log.info("결제 시드 데이터 등록 완료: {}건", paymentRepository.count());
    }
}
