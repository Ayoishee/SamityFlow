package com.samityflow.pattern.strategy;

/** Implements the AgriculturalInterest calculation policy. */
public class AgriculturalInterestStrategy implements InterestStrategy {
    @Override
    public double calculate(double principal, double yearlyRatePercent, int durationWeeks) {
        return java.math.BigDecimal.valueOf(principal)
                .multiply(java.math.BigDecimal.valueOf(yearlyRatePercent))
                .movePointLeft(2)
                .multiply(java.math.BigDecimal.valueOf(durationWeeks))
                .divide(java.math.BigDecimal.valueOf(52), 12, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }
}
