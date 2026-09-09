package com.samityflow.model;

import com.samityflow.pattern.state.ApplicationState;
import com.samityflow.pattern.state.ApplicationStates;

public class LoanApplication {
    private final int id;
    private final int memberId;
    private final int productId;
    private final double amount;
    private final String purpose;
    private ApplicationStatus status;
    private boolean officerApproved;
    private String managerComment;

    public LoanApplication(int id, int memberId, int productId, double amount, String purpose,
                           ApplicationStatus status, boolean officerApproved, String managerComment) {
        this.id = id;
        this.memberId = memberId;
        this.productId = productId;
        this.amount = amount;
        this.purpose = purpose;
        this.status = status;
        this.officerApproved = officerApproved;
        this.managerComment = managerComment == null ? "" : managerComment;
    }

    public int getId() { return id; }
    public int getMemberId() { return memberId; }
    public int getProductId() { return productId; }
    public double getAmount() { return amount; }
    public String getPurpose() { return purpose; }
    public ApplicationStatus getStatus() { return status; }
    public boolean isOfficerApproved() { return officerApproved; }
    public String getManagerComment() { return managerComment; }

    public ApplicationState state() {
        return ApplicationStates.forStatus(status);
    }

    public void submit() { state().submit(this); }
    public void guaranteesCompleted() { state().guaranteesCompleted(this); }
    public void officerApprove() { state().officerApprove(this); }
    public void managerApprove(String comment) { state().managerApprove(this, comment); }
    public void reject(String comment) { state().reject(this, comment); }

    public void changeStatus(ApplicationStatus newStatus) { this.status = newStatus; }
    public void markOfficerApproved() { this.officerApproved = true; }
    public void setManagerComment(String comment) { this.managerComment = comment == null ? "" : comment; }

    @Override
    public String toString() {
        return "Application #" + id + " - " + amount + " - " + status;
    }
}
