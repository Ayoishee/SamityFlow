package com.samityflow.service;

import com.samityflow.database.Database;
import com.samityflow.model.ApplicationStatus;
import com.samityflow.model.Installment;
import com.samityflow.model.Loan;
import com.samityflow.model.LoanApplication;
import com.samityflow.model.LoanProduct;
import com.samityflow.pattern.factory.ScheduleFactory;
import com.samityflow.pattern.schedule.Schedule;
import com.samityflow.pattern.strategy.CalculationStrategyFactory;
import com.samityflow.repository.AuditLogRepository;
import com.samityflow.repository.InstallmentRepository;
import com.samityflow.repository.LoanApplicationRepository;
import com.samityflow.repository.LoanProductRepository;
import com.samityflow.repository.LoanRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class LoanDisbursementService {
    private final Database database;
    private final Connection suppliedConnection;

    public LoanDisbursementService(Database database) {
        this.database = database;
        this.suppliedConnection = null;
    }

    public LoanDisbursementService(Connection connection) {
        this.database = null;
        this.suppliedConnection = connection;
    }

    public Loan disburse(int applicationId, LocalDate firstDueDate, String scheduleType) {
        if (firstDueDate == null) throw new IllegalArgumentException("First due date is required");
        Connection connection = null;
        boolean ownsConnection = suppliedConnection == null;

        try {
            connection = ownsConnection ? database.connect() : suppliedConnection;
            connection.setAutoCommit(false);

            LoanApplicationRepository applications = new LoanApplicationRepository(connection);
            LoanRepository loans = new LoanRepository(connection);
            InstallmentRepository installments = new InstallmentRepository(connection);
            LoanProductRepository products = new LoanProductRepository(connection);
            AuditLogRepository audit = new AuditLogRepository(connection);

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
            ).setScale(2, RoundingMode.HALF_UP);

            Loan loan = new Loan(0, applicationId, application.getMemberId(), principal.doubleValue());
            loan.setOutstanding(principal.add(interest));
            loans.save(loan);
            int loanId = loans.findByApplicationId(applicationId).orElseThrow().getId();

            Schedule schedule = new ScheduleFactory().create(scheduleType);
            List<Installment> generated = schedule.generate(
                    loanId, principal, interest, product.durationWeeks(), firstDueDate);
            installments.saveAll(generated);
            loans.updateStatus(loanId, "ACTIVE");
            audit.save("LOAN_DISBURSED", "LOAN", loanId, "SYSTEM",
                    "Application " + applicationId + " disbursed with " + scheduleType + " schedule");

            connection.commit();
            return loans.findById(loanId).orElseThrow();
        } catch (Exception exception) {
            rollback(connection);
            if (exception instanceof RuntimeException runtimeException) throw runtimeException;
            throw new RuntimeException("Loan disbursement failed", exception);
        } finally {
            resetAndClose(connection, ownsConnection);
        }
    }

    private void rollback(Connection connection) {
        if (connection != null) try { connection.rollback(); } catch (SQLException ignored) { }
    }

    private void resetAndClose(Connection connection, boolean close) {
        if (connection == null) return;
        try { connection.setAutoCommit(true); } catch (SQLException ignored) { }
        if (close) try { connection.close(); } catch (SQLException ignored) { }
    }
}
