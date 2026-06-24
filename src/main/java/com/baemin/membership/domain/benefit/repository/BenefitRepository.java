package com.baemin.membership.domain.benefit.repository;

import com.baemin.membership.domain.benefit.entity.Benefit;
import com.baemin.membership.domain.member.entity.MembershipGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BenefitRepository extends JpaRepository<Benefit, Long> {

    List<Benefit> findByRequiredGradeAndActiveTrue(MembershipGrade grade);

    List<Benefit> findByActiveTrue();
}
