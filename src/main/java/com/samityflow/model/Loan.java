package com.samityflow.model;

import java.math.BigDecimal;

public class Loan {
    private int id;
    private int applicationId;
    private int memberId;
    private BigDecimal amount = BigDecimal.ZERO;
    private BigDecimal outstanding = BigDecimal.ZERO;
    private String status = "PENDING";

    public Loan() {
    }

    public Loan(int id, int applicationId, int memberId, double amount) {
        this.id = id;
        this.applicationId = applicationId;
        this.memberId = memberId;
        this.amount = BigDecimal.valueOf(amount);
        this.outstanding = this.amount;
    }

    public int getId() {
        return id;
    }

    public int getApplicationId() {
        return applicationId;
    }

    public int getMemberId() {
        return memberId;
    }

    /**
     * Backward-compatible accessor retained for existing UI/service code.
     */
    public double getAmount() {
        return amount.doubleValue();
    }

    public BigDecimal getAmountDecimal() {
        return amount;
    }

    /**
     * Backward-compatible accessor retained for existing UI/service code.
     */
    public double getOutstanding() {
        return outstanding.doubleValue();
    }

    public BigDecimal getOutstandingDecimal() {
        return outstanding;
    }

    public void setOutstanding(double value) {
        outstanding = BigDecimal.valueOf(value);
    }

    public void setOutstanding(BigDecimal value) {
        outstanding = value == null ? BigDecimal.ZERO : value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Loan #" + id + " - outstanding " + outstanding.toPlainString() + " - " + status;
    }
}
