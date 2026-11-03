package com.samityflow.pattern.strategy;

/** Implements the NormalPenalty calculation policy. */
public class NormalPenaltyStrategy implements PenaltyStrategy {
    @Override
    public double calculate(double overdueAmount, double penaltyRatePercent, int overdueWeeks) {
        return java.math.BigDecimal.valueOf(Math.max(0, overdueAmount))
                .multiply(java.math.BigDecimal.valueOf(Math.max(0, penaltyRatePercent)))
                .movePointLeft(2)
                .multiply(java.math.BigDecimal.valueOf(Math.max(0, overdueWeeks)))
                .doubleValue();
    }
}
