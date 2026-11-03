package com.samityflow.model;

/** Defines editable product defaults; application agreements use immutable snapshots. */
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
