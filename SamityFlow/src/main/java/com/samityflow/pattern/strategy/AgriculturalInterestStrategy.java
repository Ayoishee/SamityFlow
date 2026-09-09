package com.samityflow.pattern.strategy;

public class AgriculturalInterestStrategy implements InterestStrategy {
    @Override
    public double calculate(double principal, double yearlyRatePercent, int durationWeeks) {
        double years = durationWeeks / 52.0;
        return principal * (yearlyRatePercent / 100.0) * years;
    }
}
