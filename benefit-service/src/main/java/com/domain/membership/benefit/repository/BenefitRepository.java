package com.domain.membership.benefit.repository;

import com.domain.membership.benefit.entity.Benefit;
import com.domain.membership.common.type.MembershipGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BenefitRepository extends JpaRepository<Benefit, Long> {

    List<Benefit> findByRequiredGradeAndActiveTrue(MembershipGrade grade);

    List<Benefit> findByActiveTrue();
}
