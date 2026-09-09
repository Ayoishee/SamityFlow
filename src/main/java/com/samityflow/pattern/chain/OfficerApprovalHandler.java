package com.samityflow.pattern.chain;

public class OfficerApprovalHandler extends ApprovalHandler {
    @Override
    protected void check(ApprovalRequest request) {
        if (!request.officerApproved()) throw new IllegalStateException("Field officer has not approved");
    }
}
