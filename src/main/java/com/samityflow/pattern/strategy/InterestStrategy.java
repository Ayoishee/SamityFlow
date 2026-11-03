package com.samityflow.pattern.strategy;

/** Implements the Interest calculation policy. */
public interface InterestStrategy {
    double calculate(double principal, double yearlyRatePercent, int durationWeeks);
}
