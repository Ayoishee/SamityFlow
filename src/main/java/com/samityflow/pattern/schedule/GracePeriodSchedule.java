package com.samityflow.pattern.schedule;

import com.samityflow.model.Installment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class GracePeriodSchedule implements Schedule {


    private final int graceWeeks;


    public GracePeriodSchedule(int graceWeeks) {
        this.graceWeeks = graceWeeks;
    }


    @Override
    public List<Installment> generate(
            int loanId,
            BigDecimal principalAmount,
            BigDecimal interestAmount,
            int installmentCount,
            LocalDate firstDueDate
    ) {


        StandardWeeklySchedule schedule =
                new StandardWeeklySchedule();


        return schedule.generate(
                loanId,
                principalAmount,
                interestAmount,
                installmentCount,
                firstDueDate.plusWeeks(graceWeeks)
        );
    }
}