package com.samityflow.pattern.strategy;

public class GracePeriodPenaltyStrategy implements PenaltyStrategy {
    @Override
    public double calculate(double overdueAmount, double penaltyRatePercent, int overdueWeeks) {
        int chargeableWeeks = Math.max(0, overdueWeeks - 1);
        return overdueAmount * (penaltyRatePercent / 100.0) * chargeableWeeks;
    }
}
