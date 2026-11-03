package com.samityflow.pattern.schedule;

import com.samityflow.model.Installment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Generates dated repayment installments with cent-rounded residual allocation. */
public interface Schedule {

    List<Installment> generate(
            int loanId,
            BigDecimal principalAmount,
            BigDecimal interestAmount,
            int installmentCount,
            LocalDate firstDueDate);
}
