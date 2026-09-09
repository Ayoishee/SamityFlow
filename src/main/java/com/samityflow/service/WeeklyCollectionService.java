package com.samityflow.service;

import com.samityflow.database.Database;
import com.samityflow.repository.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;

public class WeeklyCollectionService {

    private final Database database;

    public WeeklyCollectionService() {
        this(Database.applicationDatabase());
    }

    public WeeklyCollectionService(Database database) {
        this.database = database;
    }

    public CollectionResult collectPayment(
            int installmentId,
            int loanId,
            int memberId,
            BigDecimal paymentAmount,
            BigDecimal savingsAmount,
            LocalDate date) {

        if (paymentAmount == null ||
                paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new CollectionResult(false,
                    "Payment amount must be positive");
        }

        try (Connection c = database.connect()) {

            c.setAutoCommit(false);

            try {
                InstallmentRepository installments =
                        new InstallmentRepository(c);

                PaymentRepository payments =
                        new PaymentRepository(c);

                LoanRepository loans =
                        new LoanRepository(c);

                SavingsTransactionRepository savings =
                        new SavingsTransactionRepository(c);

                AuditLogRepository audit =
                        new AuditLogRepository(c);

                var item = installments.findById(installmentId);

                if (item.isEmpty()) {
                    throw new RuntimeException("Installment not found");
                }

                var current = item.get();

                BigDecimal outstandingInstallment =
                        current.getTotalAmount()
                                .subtract(current.getPaidAmount());

                if (paymentAmount.compareTo(outstandingInstallment) > 0) {
                    throw new RuntimeException(
                            "Payment exceeds outstanding amount");
                }

                BigDecimal newPaid =
                        current.getPaidAmount()
                                .add(paymentAmount);

                String status =
                        newPaid.compareTo(current.getTotalAmount()) >= 0
                                ? "PAID"
                                : "PARTIALLY_PAID";

                payments.save(
                        installmentId,
                        loanId,
                        memberId,
                        paymentAmount.doubleValue(),
                        "WEEKLY_COLLECTION"
                );

                installments.updatePayment(
                        installmentId,
                        newPaid,
                        status
                );

                // FIX: decrease existing loan outstanding balance
                var loan = loans.findById(loanId)
                        .orElseThrow(() ->
                                new RuntimeException("Loan not found"));

                BigDecimal newLoanOutstanding =
                        loan.getOutstandingDecimal()
                                .subtract(paymentAmount);

                if (newLoanOutstanding.compareTo(BigDecimal.ZERO) < 0) {
                    newLoanOutstanding = BigDecimal.ZERO;
                }

                loans.updateOutstandingBalance(
                        loanId,
                        newLoanOutstanding
                );

                if (savingsAmount != null &&
                        savingsAmount.compareTo(BigDecimal.ZERO) > 0) {

                    savings.save(
                            memberId,
                            "DEPOSIT",
                            savingsAmount.doubleValue(),
                            "WEEKLY_COLLECTION"
                    );
                }

                audit.save(
                        "PAYMENT_COLLECTION",
                        "INSTALLMENT",
                        installmentId,
                        "SYSTEM",
                        "Weekly collection posted"
                );

                c.commit();

                return new CollectionResult(
                        true,
                        "Collection successful"
                );

            } catch (Exception e) {
                c.rollback();
                return new CollectionResult(false, e.getMessage());
            }

        } catch (Exception e) {
            return new CollectionResult(false, e.getMessage());
        }
    }
}