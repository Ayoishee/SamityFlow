package com.samityflow.pattern.chain;

public record ApprovalRequest(
        boolean memberEligible,
        int collectedGuarantees,
        int requiredGuarantees,
        boolean officerApproved,
        boolean managerApproved) {
}
