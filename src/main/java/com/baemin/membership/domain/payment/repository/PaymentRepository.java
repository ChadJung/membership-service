package com.baemin.membership.domain.payment.repository;

import com.baemin.membership.domain.payment.entity.Payment;
import com.baemin.membership.domain.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByMemberIdOrderByPaymentDateDesc(Long memberId);

    Optional<Payment> findByTransactionId(String transactionId);

    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.nextPaymentDate <= :now")
    List<Payment> findPaymentsDueForRenewal(
            @Param("status") PaymentStatus status,
            @Param("now") LocalDateTime now
    );
}
