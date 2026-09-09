package com.samityflow.service;

import com.samityflow.model.LoanProduct;
import com.samityflow.pattern.strategy.CalculationStrategyFactory;

public class LoanCalculationService {
    public double interest(LoanProduct product, double amount) {
        return CalculationStrategyFactory.interest(product.interestStrategy())
                .calculate(amount, product.interestRate(), product.durationWeeks());
    }

    public double totalPayable(LoanProduct product, double amount) {
        return amount + interest(product, amount);
    }

    public double penalty(LoanProduct product, double overdueAmount, int overdueWeeks) {
        return CalculationStrategyFactory.penalty(product.penaltyStrategy())
                .calculate(overdueAmount, product.penaltyRate(), overdueWeeks);
    }
}
