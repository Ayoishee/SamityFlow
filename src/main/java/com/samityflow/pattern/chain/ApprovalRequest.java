package com.samityflow.pattern.chain;

/** Represents ApprovalRequest within the microfinance workflow. */
public record ApprovalRequest(
        boolean memberEligible,
        int collectedGuarantees,
        int requiredGuarantees,
        boolean officerApproved,
        boolean managerApproved) {}
