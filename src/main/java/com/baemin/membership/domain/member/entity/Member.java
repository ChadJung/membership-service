package com.baemin.membership.domain.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipGrade grade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipStatus status;

    @Column(nullable = false)
    private LocalDateTime subscribedAt;

    private LocalDateTime expiredAt;

    private LocalDateTime cancelledAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Member(Long userId, MembershipGrade grade) {
        this.userId = userId;
        this.grade = grade;
        this.status = MembershipStatus.ACTIVE;
        this.subscribedAt = LocalDateTime.now();
        this.expiredAt = LocalDateTime.now().plusMonths(1);
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status != MembershipStatus.ACTIVE) {
            throw new IllegalStateException("활성 상태의 멤버십만 해지할 수 있습니다.");
        }
        this.status = MembershipStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void renew() {
        this.status = MembershipStatus.ACTIVE;
        this.expiredAt = LocalDateTime.now().plusMonths(1);
        this.cancelledAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return this.expiredAt != null && this.expiredAt.isBefore(LocalDateTime.now());
    }

    public void upgradeGrade(MembershipGrade newGrade) {
        if (this.grade == newGrade) {
            throw new IllegalArgumentException("동일한 등급으로 변경할 수 없습니다.");
        }
        this.grade = newGrade;
        this.updatedAt = LocalDateTime.now();
    }
}
