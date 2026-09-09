package com.samityflow.pattern.chain;

public abstract class ApprovalHandler {
    private ApprovalHandler next;

    public ApprovalHandler setNext(ApprovalHandler next) {
        this.next = next;
        return next;
    }

    public void handle(ApprovalRequest request) {
        check(request);
        if (next != null) next.handle(request);
    }

    protected abstract void check(ApprovalRequest request);
}
