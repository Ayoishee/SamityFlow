package com.samityflow.pattern.chain;

public class GuaranteeHandler extends ApprovalHandler {
    @Override
    protected void check(ApprovalRequest request) {
        if (request.collectedGuarantees() < request.requiredGuarantees()) {
            throw new IllegalStateException("Required guarantees are not collected");
        }
    }
}
