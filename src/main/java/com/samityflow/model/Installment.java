package com.samityflow.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Installment {

    private int id;
    private int loanId;
    private int number;

    private LocalDate dueDate;

    private BigDecimal principalAmount = BigDecimal.ZERO;
    private BigDecimal interestAmount = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    private BigDecimal penaltyAmount = BigDecimal.ZERO;
    private BigDecimal paidAmount = BigDecimal.ZERO;

    private String status = "PENDING";


    // Existing constructor preserved
    public Installment() {
    }


    // Existing constructor preserved for backward compatibility
    public Installment(int id, int loanId, int number, double amount) {
        this.id = id;
        this.loanId = loanId;
        this.number = number;
        this.totalAmount = BigDecimal.valueOf(amount);
    }


    public Installment(
            int loanId,
            int number,
            LocalDate dueDate,
            BigDecimal principal,
            BigDecimal interest
    ) {
        this.loanId = loanId;
        this.number = number;
        this.dueDate = dueDate;

        this.principalAmount = principal;
        this.interestAmount = interest;

        this.totalAmount =
                principal.add(interest);
    }


    public int getId() {
        return id;
    }


    public int getLoanId() {
        return loanId;
    }


    public int getNumber() {
        return number;
    }


    public LocalDate getDueDate() {
        return dueDate;
    }


    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }


    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }


    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }


    public BigDecimal getInterestAmount() {
        return interestAmount;
    }


    public void setInterestAmount(BigDecimal interestAmount) {
        this.interestAmount = interestAmount;
    }


    public BigDecimal getTotalAmount() {
        return totalAmount;
    }


    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }


    public BigDecimal getPenaltyAmount() {
        return penaltyAmount;
    }


    public void setPenaltyAmount(BigDecimal penaltyAmount) {
        this.penaltyAmount = penaltyAmount;
    }


    public BigDecimal getPaidAmount() {
        return paidAmount;
    }


    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }


    public String getStatus() {
        return status;
    }


    public void setStatus(String status) {
        this.status = status;
    }
}