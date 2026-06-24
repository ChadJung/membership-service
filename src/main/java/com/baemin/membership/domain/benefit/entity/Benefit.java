package com.baemin.membership.domain.benefit.entity;

import com.baemin.membership.domain.member.entity.MembershipGrade;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "benefits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Benefit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BenefitType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipGrade requiredGrade;

    @Column(nullable = false)
    private int discountValue;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Benefit(String name, String description, BenefitType type,
                   MembershipGrade requiredGrade, int discountValue) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.requiredGrade = requiredGrade;
        this.discountValue = discountValue;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
    }
}
