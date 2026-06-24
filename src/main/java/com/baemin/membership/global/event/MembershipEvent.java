package com.baemin.membership.global.event;

import com.baemin.membership.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MembershipEvent {

    private final String type;
    private final Long userId;
    private final String grade;

    public static MembershipEvent subscribed(Member member) {
        return new MembershipEvent("SUBSCRIBED", member.getUserId(), member.getGrade().name());
    }

    public static MembershipEvent cancelled(Member member) {
        return new MembershipEvent("CANCELLED", member.getUserId(), member.getGrade().name());
    }
}
