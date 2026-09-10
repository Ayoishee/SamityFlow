package com.samityflow.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepaymentServiceTest {

    @Test
    void generatesTheRequestedNumberOfWeeklyPayments() {
        RepaymentService service = new RepaymentService();

        List<Double> schedule = service.generateWeeklySchedule(1_200, 12);

        assertEquals(12, schedule.size());
        assertEquals(100, schedule.get(0), 0.001);
    }

    @Test
    void generatedPaymentsAddUpToTheLoanAmount() {
        RepaymentService service = new RepaymentService();

        List<Double> schedule = service.generateWeeklySchedule(1_000, 10);
        double total = schedule.stream().mapToDouble(Double::doubleValue).sum();

        assertEquals(1_000, total, 0.001);
    }

    @Test
    void rejectsInvalidAmountAndDuration() {
        RepaymentService service = new RepaymentService();
        assertThrows(IllegalArgumentException.class,
                () -> service.generateWeeklySchedule(1_000, 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.generateWeeklySchedule(0, 10));
    }
}
