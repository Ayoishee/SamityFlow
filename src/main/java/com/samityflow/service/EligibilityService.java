package com.samityflow.service;

import com.samityflow.model.Member;
import com.samityflow.repository.MemberRepository;

/** Checks whether a member and their organizational group may apply for a loan. */
public class EligibilityService {
    private final MemberRepository members;

    public EligibilityService(MemberRepository members) {
        this.members = members;
    }

    /** Checks member activity, group and samity activity and the member eligibility flag. */
    public boolean isEligible(Member member) {
        return member.eligible() && member.active() && members.belongsToActiveGroup(member.id());
    }
}
