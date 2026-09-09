package com.samityflow.pattern.strategy;

public class BusinessInterestStrategy implements InterestStrategy {
    @Override
    public double calculate(double principal, double yearlyRatePercent, int durationWeeks) {
        double weeklyRate = (yearlyRatePercent / 100.0) / 52.0;
        return principal * weeklyRate * durationWeeks;
    }
}
