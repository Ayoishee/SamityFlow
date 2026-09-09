package com.samityflow.pattern;

import com.samityflow.model.Installment;
import com.samityflow.pattern.factory.ScheduleFactory;
import com.samityflow.pattern.schedule.GracePeriodSchedule;
import com.samityflow.pattern.schedule.Schedule;
import com.samityflow.pattern.schedule.SeasonalSchedule;
import com.samityflow.pattern.schedule.StandardWeeklySchedule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScheduleFactoryTest {

    private final ScheduleFactory factory = new ScheduleFactory();

    @Test
    void factoryCreatesEachScheduleType() {
        assertInstanceOf(StandardWeeklySchedule.class, factory.create("WEEKLY"));
        assertInstanceOf(GracePeriodSchedule.class, factory.create("GRACE"));
        assertInstanceOf(SeasonalSchedule.class, factory.create("SEASONAL"));
    }

    @Test
    void factoryRejectsMissingAndUnknownTypes() {
        assertThrows(IllegalArgumentException.class, () -> factory.create(null));

        assertThrows(
                IllegalArgumentException.class,
                () -> factory.create("UNKNOWN")
        );
    }

    @Test
    void weeklyScheduleUsesSevenDayIntervalsAndPreservesTotals() {
        LocalDate firstDueDate = LocalDate.of(2026, 1, 4);
        Schedule schedule = factory.create("WEEKLY");

        List<Installment> installments = schedule.generate(
                10,
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                3,
                firstDueDate
        );

        assertEquals(3, installments.size());
        assertEquals(firstDueDate, installments.get(0).getDueDate());
        assertEquals(firstDueDate.plusWeeks(1), installments.get(1).getDueDate());
        assertEquals(firstDueDate.plusWeeks(2), installments.get(2).getDueDate());

        BigDecimal principalTotal = installments.stream()
                .map(Installment::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal interestTotal = installments.stream()
                .map(Installment::getInterestAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, new BigDecimal("1000.00").compareTo(principalTotal));
        assertEquals(0, new BigDecimal("100.00").compareTo(interestTotal));
    }

    @Test
    void graceScheduleDelaysTheFirstPaymentByTwoWeeks() {
        LocalDate firstDueDate = LocalDate.of(2026, 1, 4);

        List<Installment> installments = factory.create("GRACE").generate(
                10,
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                2,
                firstDueDate
        );

        assertEquals(firstDueDate.plusWeeks(2), installments.get(0).getDueDate());
    }

    @Test
    void seasonalScheduleUsesThreeMonthIntervals() {
        LocalDate firstDueDate = LocalDate.of(2026, 1, 4);

        List<Installment> installments = factory.create("SEASONAL").generate(
                10,
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                2,
                firstDueDate
        );
        assertEquals(firstDueDate, installments.get(0).getDueDate());
        assertEquals(firstDueDate.plusMonths(3), installments.get(1).getDueDate());
    }
}
