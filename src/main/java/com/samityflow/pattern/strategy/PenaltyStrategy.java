package com.samityflow.pattern.strategy;

public interface PenaltyStrategy {
    double calculate(double overdueAmount, double penaltyRatePercent, int overdueWeeks);
}
