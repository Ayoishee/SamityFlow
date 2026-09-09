package com.samityflow.pattern.strategy;

public class NormalPenaltyStrategy implements PenaltyStrategy {
    @Override
    public double calculate(double overdueAmount, double penaltyRatePercent, int overdueWeeks) {
        return overdueAmount * (penaltyRatePercent / 100.0) * overdueWeeks;
    }
}
