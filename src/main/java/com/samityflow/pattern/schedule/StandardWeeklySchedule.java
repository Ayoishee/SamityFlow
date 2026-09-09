package com.samityflow.pattern.schedule;

import com.samityflow.model.Installment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StandardWeeklySchedule implements Schedule {

    private static final int SCALE = 2;


    @Override
    public List<Installment> generate(
            int loanId,
            BigDecimal principalAmount,
            BigDecimal interestAmount,
            int installmentCount,
            LocalDate firstDueDate
    ) {

        List<Installment> installments = new ArrayList<>();

        BigDecimal principalPerInstallment =
                principalAmount
                        .divide(
                                BigDecimal.valueOf(installmentCount),
                                SCALE,
                                RoundingMode.HALF_UP
                        );


        BigDecimal interestPerInstallment =
                interestAmount
                        .divide(
                                BigDecimal.valueOf(installmentCount),
                                SCALE,
                                RoundingMode.HALF_UP
                        );


        BigDecimal principalTotal = BigDecimal.ZERO;
        BigDecimal interestTotal = BigDecimal.ZERO;


        for (int i = 1; i <= installmentCount; i++) {


            BigDecimal principal = principalPerInstallment;
            BigDecimal interest = interestPerInstallment;


            // Last installment absorbs rounding difference
            if (i == installmentCount) {

                principal =
                        principalAmount.subtract(principalTotal);

                interest =
                        interestAmount.subtract(interestTotal);
            }


            LocalDate dueDate =
                    firstDueDate.plusDays(
                            7L * (i - 1)
                    );


            Installment installment =
                    new Installment(
                            loanId,
                            i,
                            dueDate,
                            principal,
                            interest
                    );


            installment.setPenaltyAmount(
                    BigDecimal.ZERO
            );

            installment.setPaidAmount(
                    BigDecimal.ZERO
            );

            installment.setStatus(
                    "PENDING"
            );


            installments.add(installment);


            principalTotal =
                    principalTotal.add(principal);

            interestTotal =
                    interestTotal.add(interest);
        }


        return installments;
    }
}