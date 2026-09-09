
package com.samityflow.service;

import com.samityflow.model.*;
import com.samityflow.pattern.factory.ScheduleFactory;
import com.samityflow.pattern.schedule.Schedule;
import com.samityflow.pattern.strategy.CalculationStrategyFactory;
import com.samityflow.repository.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class LoanDisbursementService {
    private final Connection connection;
    private final LoanApplicationRepository applications;
    private final LoanRepository loans;
    private final InstallmentRepository installments;
    private final LoanProductRepository products;
    private final AuditLogRepository audit;

    public LoanDisbursementService(Connection connection) {
        this.connection = connection;
        this.applications = new LoanApplicationRepository(connection);
        this.loans = new LoanRepository(connection);
        this.installments = new InstallmentRepository(connection);
        this.products = new LoanProductRepository(connection);
        this.audit = new AuditLogRepository(connection);
    }

    public Loan disburse(int applicationId, LocalDate firstDueDate, String scheduleType) {
        try {
            connection.setAutoCommit(false);
            LoanApplication application = applications.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found"));

            if (application.getStatus() != ApplicationStatus.APPROVED) {
                throw new IllegalStateException("Only approved applications can be disbursed");
            }

            if (loans.findByApplicationId(applicationId).isPresent()) {
                throw new IllegalStateException("Application already disbursed");
            }

            LoanProduct product = products.findById(application.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));

            BigDecimal principal = BigDecimal.valueOf(application.getAmount());
            BigDecimal interest = BigDecimal.valueOf(
                    CalculationStrategyFactory.interest(product.interestStrategy())
                            .calculate(application.getAmount(), product.interestRate(), product.durationWeeks())
            ).setScale(2, java.math.RoundingMode.HALF_UP);

            Loan loan = new Loan(0, applicationId, application.getMemberId(), principal.doubleValue());
            loans.save(loan);

            int loanId = loans.findByApplicationId(applicationId)
                    .orElseThrow().getId();

            Schedule schedule = new ScheduleFactory().create(scheduleType);
            List<Installment> generated = schedule.generate(
                    loanId, principal, interest, product.durationWeeks(), firstDueDate);

            installments.saveAll(generated);
            loans.updateStatus(loanId, "ACTIVE");
            audit.save("LOAN_DISBURSED", "LOAN", loanId, "system",
                    "Application " + applicationId + " disbursed");
            connection.commit();
            return loans.findById(loanId).orElse(loan);
        } catch (Exception e) {
            try { connection.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException(e);
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }
}
