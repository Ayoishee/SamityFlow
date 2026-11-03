package com.samityflow.pattern.strategy;

/** Implements the Penalty calculation policy. */
public interface PenaltyStrategy {
    double calculate(double overdueAmount, double penaltyRatePercent, int overdueWeeks);
}
