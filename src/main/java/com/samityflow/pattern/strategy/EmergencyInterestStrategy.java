package com.samityflow.pattern.strategy;

/** Implements the EmergencyInterest calculation policy. */
public class EmergencyInterestStrategy implements InterestStrategy {
    @Override
    public double calculate(double principal, double yearlyRatePercent, int durationWeeks) {
        return java.math.BigDecimal.valueOf(principal)
                .multiply(java.math.BigDecimal.valueOf(yearlyRatePercent))
                .movePointLeft(2)
                .doubleValue();
    }
}
