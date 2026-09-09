package com.samityflow.pattern.strategy;

public class EmergencyInterestStrategy implements InterestStrategy {
    @Override
    public double calculate(double principal, double yearlyRatePercent, int durationWeeks) {
        // Emergency product uses one flat charge, independent of duration.
        return principal * (yearlyRatePercent / 100.0);
    }
}
