package com.baemin.membership.domain.member.repository;

import com.baemin.membership.domain.member.entity.Member;
import com.baemin.membership.domain.member.entity.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUserId(Long userId);

    Optional<Member> findByUserIdAndStatus(Long userId, MembershipStatus status);

    boolean existsByUserIdAndStatus(Long userId, MembershipStatus status);

    @Query("SELECT m FROM Member m WHERE m.status = :status AND m.expiredAt < CURRENT_TIMESTAMP")
    List<Member> findExpiredMembers(@Param("status") MembershipStatus status);

    long countByStatus(MembershipStatus status);
}
