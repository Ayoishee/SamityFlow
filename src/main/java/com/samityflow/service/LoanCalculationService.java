package com.samityflow.service;

import com.samityflow.model.LoanProduct;
import com.samityflow.pattern.strategy.CalculationStrategyFactory;

/** Provides interest and penalty quotes using product calculation strategies. */
public class LoanCalculationService {
    /** Calculates persisted interest in decimal arithmetic, rounding once to two places HALF_UP. */
    public java.math.BigDecimal interestDecimal(
            LoanProduct product, java.math.BigDecimal principal) {
        java.math.BigDecimal charge =
                principal
                        .multiply(java.math.BigDecimal.valueOf(product.interestRate()))
                        .movePointLeft(2);
        if (!"EMERGENCY".equals(product.interestStrategy())) {
            charge =
                    charge.multiply(java.math.BigDecimal.valueOf(product.durationWeeks()))
                            .divide(
                                    java.math.BigDecimal.valueOf(52),
                                    12,
                                    java.math.RoundingMode.HALF_UP);
        }
        return com.samityflow.model.Money.round(charge);
    }

    /**
     * Calculates the product interest strategy for a principal and duration; compatibility API
     * returns double.
     */
    public double interest(LoanProduct product, double amount) {
        return CalculationStrategyFactory.interest(product.interestStrategy())
                .calculate(amount, product.interestRate(), product.durationWeeks());
    }

    /**
     * Returns principal plus calculated interest for a quote; disbursement rounds persisted amounts
     * to cents.
     */
    public double totalPayable(LoanProduct product, double amount) {
        return amount + interest(product, amount);
    }

    /**
     * Quotes cumulative penalty for overdue base debt and weeks; persisted scans charge only
     * unassessed weeks.
     */
    public double penalty(LoanProduct product, double overdueAmount, int overdueWeeks) {
        return CalculationStrategyFactory.penalty(product.penaltyStrategy())
                .calculate(overdueAmount, product.penaltyRate(), overdueWeeks);
    }
}
