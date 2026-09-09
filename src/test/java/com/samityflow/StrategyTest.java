package com.samityflow;

import com.samityflow.pattern.strategy.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StrategyTest {
    @Test
    void agriculturalInterestUsesSimpleInterestForTheTerm() {
        InterestStrategy strategy = new AgriculturalInterestStrategy();
        assertEquals(400.0, strategy.calculate(10_000, 8, 26), 0.01);
    }

    @Test
    void emergencyInterestUsesOneFlatCharge() {
        InterestStrategy strategy = new EmergencyInterestStrategy();
        assertEquals(500.0, strategy.calculate(10_000, 5, 12), 0.01);
    }

    @Test
    void gracePenaltyDoesNotChargeTheFirstOverdueWeek() {
        PenaltyStrategy strategy = new GracePeriodPenaltyStrategy();
        assertEquals(0, strategy.calculate(1_000, 2, 1), 0.01);
        assertEquals(40, strategy.calculate(1_000, 2, 3), 0.01);
    }
}
