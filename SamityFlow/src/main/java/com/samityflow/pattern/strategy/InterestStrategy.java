package com.samityflow.pattern.strategy;

public interface InterestStrategy {
    double calculate(double principal, double yearlyRatePercent, int durationWeeks);
}
