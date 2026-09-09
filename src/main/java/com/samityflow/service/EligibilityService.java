package com.samityflow.service;

import com.samityflow.model.Member;
import com.samityflow.repository.MemberRepository;

public class EligibilityService {
    private final MemberRepository members;
    public EligibilityService(MemberRepository members) { this.members = members; }

    public boolean isEligible(Member member) {
        return member.eligible() && member.active() && members.belongsToActiveGroup(member.id());
    }
}
