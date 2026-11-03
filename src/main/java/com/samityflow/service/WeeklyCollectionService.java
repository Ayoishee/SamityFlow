package com.samityflow.service;

import com.samityflow.database.Database;
import com.samityflow.model.LedgerValue;
import com.samityflow.repository.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.UUID;

/** Posts repayment, linked savings and audit history in a single transaction. */
public class WeeklyCollectionService {

    private final Database database;

    public WeeklyCollectionService() {
        this(Database.applicationDatabase());
    }

    public WeeklyCollectionService(Database database) {
        this.database = database;
    }

    /**
     * Posts a positive repayment, optional nonnegative savings, and audit entry atomically. Amounts
     * round to cents; identifiers must describe the same loan and member. Returns a failure result
     * without persisted changes on validation or database errors. Owns and closes its connection.
     */
    public CollectionResult collectPayment(
            int installmentId,
            int loanId,
            int memberId,
            BigDecimal paymentAmount,
            BigDecimal savingsAmount,
            LocalDate date) {

        return collectPayment(
                installmentId,
                loanId,
                memberId,
                paymentAmount,
                savingsAmount,
                date,
                "COL-" + UUID.randomUUID());
    }

    /**
     * Posts a positive repayment, optional nonnegative savings, and audit entry atomically. Amounts
     * round to cents; identifiers must describe the same loan and member. Returns a failure result
     * without persisted changes on validation or database errors. Owns and closes its connection.
     */
    public CollectionResult collectPayment(
            int installmentId,
            int loanId,
            int memberId,
            BigDecimal paymentAmount,
            BigDecimal savingsAmount,
            LocalDate date,
            String reference) {

        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new CollectionResult(false, "Payment amount must be positive");
        }
        if (savingsAmount != null && savingsAmount.compareTo(BigDecimal.ZERO) < 0) {
            return new CollectionResult(false, "Savings amount cannot be negative");
        }
        if (reference == null || reference.isBlank()) {
            return new CollectionResult(false, "Collection reference is required");
        }
        if (date == null) {
            return new CollectionResult(false, "Collection date is required");
        }

        paymentAmount = com.samityflow.model.Money.round(paymentAmount);
        if (paymentAmount.signum() <= 0) {
            return new CollectionResult(false, "Payment rounds to zero");
        }
        try (Connection c = database.connect()) {

            c.setAutoCommit(false);

            try {
                InstallmentRepository installments = new InstallmentRepository(c);

                PaymentRepository payments = new PaymentRepository(c);

                LoanRepository loans = new LoanRepository(c);

                SavingsTransactionRepository savings = new SavingsTransactionRepository(c);

                AuditLogRepository audit = new AuditLogRepository(c);

                var item = installments.findById(installmentId);

                if (item.isEmpty()) {
                    throw new RuntimeException("Installment not found");
                }

                var current = item.get();

                if (current.getLoanId() != loanId) {
                    throw new RuntimeException("Installment does not belong to this loan");
                }

                var loan =
                        loans.findById(loanId)
                                .orElseThrow(() -> new RuntimeException("Loan not found"));
                if (loan.getMemberId() != memberId) {
                    throw new RuntimeException("Loan does not belong to this member");
                }
                if (!LedgerValue.ACTIVE.equals(loan.getStatus())
                        && !LedgerValue.DEFAULTED.equals(loan.getStatus())) {
                    throw new RuntimeException("Only an active loan can receive payments");
                }
                if (payments.referenceExists(reference)) {
                    throw new RuntimeException("Collection reference already used");
                }

                BigDecimal outstandingInstallment =
                        current.getTotalAmount()
                                .add(current.getPenaltyAmount())
                                .subtract(current.getPaidAmount());

                if (paymentAmount.compareTo(outstandingInstallment) > 0) {
                    throw new RuntimeException("Payment exceeds outstanding amount");
                }

                BigDecimal newPaid = current.getPaidAmount().add(paymentAmount);

                String status =
                        newPaid.compareTo(current.getTotalAmount().add(current.getPenaltyAmount()))
                                        >= 0
                                ? LedgerValue.PAID
                                : LedgerValue.PARTIALLY_PAID;

                int paymentId =
                        payments.saveAndReturnId(
                                installmentId, loanId, memberId, paymentAmount, reference, date);

                installments.updatePayment(installmentId, newPaid, status);

                // FIX: decrease existing loan outstanding balance
                BigDecimal newLoanOutstanding =
                        loan.getOutstandingDecimal().subtract(paymentAmount);

                if (newLoanOutstanding.compareTo(BigDecimal.ZERO) < 0) {
                    newLoanOutstanding = BigDecimal.ZERO;
                }

                loans.updateOutstandingBalance(loanId, newLoanOutstanding);
                if (newLoanOutstanding.compareTo(BigDecimal.ZERO) == 0) {
                    loans.updateStatus(loanId, LedgerValue.COMPLETED);
                }

                if (savingsAmount != null && savingsAmount.compareTo(BigDecimal.ZERO) > 0) {

                    savings.saveLinked(memberId, savingsAmount, reference, paymentId);
                }

                audit.save(
                        "PAYMENT_COLLECTION",
                        "INSTALLMENT",
                        installmentId,
                        "SYSTEM",
                        "Collection " + reference + " posted");

                c.commit();

                return new CollectionResult(true, "Collection successful");

            } catch (Exception e) {
                c.rollback();
                return new CollectionResult(false, e.getMessage());
            }

        } catch (Exception e) {
            return new CollectionResult(false, e.getMessage());
        }
    }
}
