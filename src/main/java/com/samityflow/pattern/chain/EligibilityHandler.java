package com.samityflow.pattern.chain;

/** Validates its approval stage before forwarding the request along the chain. */
public class EligibilityHandler extends ApprovalHandler {
    @Override
    protected void check(ApprovalRequest request) {
        if (!request.memberEligible()) {
            throw new IllegalStateException("Member is not eligible");
        }
    }
}
