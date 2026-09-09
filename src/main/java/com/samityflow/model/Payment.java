package com.samityflow.model;

public class Payment {
    private int id;
    private int loanId;
    private int installmentId;
    private int memberId;
    private double amount;
    private String reference;
    private String status;
    private Integer reversalOfPaymentId;

    public Payment() {
    }

    // Existing constructor preserved for backward compatibility.
    public Payment(int id, int loanId, double amount, String reference) {
        this.id = id;
        this.loanId = loanId;
        this.amount = amount;
        this.reference = reference;
    }

    public Payment(
            int id,
            int loanId,
            int installmentId,
            int memberId,
            double amount,
            String reference,
            String status,
            Integer reversalOfPaymentId
    ) {
        this.id = id;
        this.loanId = loanId;
        this.installmentId = installmentId;
        this.memberId = memberId;
        this.amount = amount;
        this.reference = reference;
        this.status = status;
        this.reversalOfPaymentId = reversalOfPaymentId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLoanId() {
        return loanId;
    }

    public void setLoanId(int loanId) {
        this.loanId = loanId;
    }

    public int getInstallmentId() {
        return installmentId;
    }

    public void setInstallmentId(int installmentId) {
        this.installmentId = installmentId;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getReversalOfPaymentId() {
        return reversalOfPaymentId;
    }

    public void setReversalOfPaymentId(Integer reversalOfPaymentId) {
        this.reversalOfPaymentId = reversalOfPaymentId;
    }
}
