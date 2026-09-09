package com.samityflow.model;

public record LoanProduct(
        int id,
        String name,
        String interestStrategy,
        double interestRate,
        String penaltyStrategy,
        double penaltyRate,
        int durationWeeks,
        int requiredGuarantees,
        double weeklySavings) {

    @Override
    public String toString() {
        return name;
    }
}
