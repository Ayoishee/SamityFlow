package com.samityflow.pattern.strategy;

public final class CalculationStrategyFactory {
    private CalculationStrategyFactory() { }

    public static InterestStrategy interest(String name) {
        return switch (name) {
            case "AGRICULTURAL" -> new AgriculturalInterestStrategy();
            case "BUSINESS" -> new BusinessInterestStrategy();
            case "EMERGENCY" -> new EmergencyInterestStrategy();
            default -> throw new IllegalArgumentException("Unknown interest strategy: " + name);
        };
    }

    public static PenaltyStrategy penalty(String name) {
        return switch (name) {
            case "NORMAL" -> new NormalPenaltyStrategy();
            case "GRACE" -> new GracePeriodPenaltyStrategy();
            default -> throw new IllegalArgumentException("Unknown penalty strategy: " + name);
        };
    }
}
