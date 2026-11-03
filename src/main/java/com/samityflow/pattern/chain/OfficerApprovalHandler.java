package com.samityflow.pattern.chain;

/** Validates its approval stage before forwarding the request along the chain. */
public class OfficerApprovalHandler extends ApprovalHandler {
    @Override
    protected void check(ApprovalRequest request) {
        if (!request.officerApproved()) {
            throw new IllegalStateException("Field officer has not approved");
        }
    }
}
