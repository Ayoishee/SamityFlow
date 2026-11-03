package com.samityflow.pattern.chain;

/** Validates its approval stage before forwarding the request along the chain. */
public class ManagerApprovalHandler extends ApprovalHandler {
    @Override
    protected void check(ApprovalRequest request) {
        if (!request.managerApproved()) {
            throw new IllegalStateException("Manager has not approved");
        }
    }
}
