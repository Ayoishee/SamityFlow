package com.samityflow.pattern.command;

import com.samityflow.model.Installment;
import com.samityflow.model.Loan;
import com.samityflow.model.Payment;
import com.samityflow.repository.AuditLogRepository;
import com.samityflow.repository.InstallmentRepository;
import com.samityflow.repository.LoanRepository;
import com.samityflow.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReversePaymentCommand implements PaymentCommand {

    private final Payment payment;
    private final PaymentRepository payments;
    private final InstallmentRepository installments;
    private final LoanRepository loans;
    private final AuditLogRepository audit;
    private int reversalPaymentId;

    public ReversePaymentCommand(
            Payment payment,
            PaymentRepository payments,
            InstallmentRepository installments,
            LoanRepository loans,
            AuditLogRepository audit
    ) {
        this.payment = payment;
        this.payments = payments;
        this.installments = installments;
        this.loans = loans;
        this.audit = audit;
    }

    @Override
    public void execute() {
        try {
            validatePayment();

            Installment installment = installments
                    .findById(payment.getInstallmentId())
                    .orElseThrow(() -> new IllegalStateException("Installment not found"));

            Loan loan = loans
                    .findById(payment.getLoanId())
                    .orElseThrow(() -> new IllegalStateException("Loan not found"));

            if (installment == null) {
                throw new IllegalStateException("Installment not found");
            }

            BigDecimal paymentAmount = payment.getAmountDecimal();
            BigDecimal currentPaid = installment.getPaidAmount();
            BigDecimal restoredPaid = currentPaid.subtract(paymentAmount);

            if (restoredPaid.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException(
                        "Reversal would make installment paid amount negative"
                );
            }

            BigDecimal currentOutstanding = loan.getOutstandingDecimal();
            if (currentOutstanding.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("Loan outstanding balance is invalid");
            }

            BigDecimal restoredOutstanding = currentOutstanding.add(paymentAmount);
            String restoredStatus = installmentStatusAfterReversal(
                    installment,
                    restoredPaid
            );

            // All mutations are performed on the same transaction-owned connection.
            // The service commits only after the audit entry succeeds.
            reversalPaymentId = payments.saveReversal(payment);
            payments.updateStatus(payment.getId(), "REVERSED");
            installments.updatePayment(
                    payment.getInstallmentId(),
                    restoredPaid,
                    restoredStatus
            );
            loans.updateOutstandingBalance(
                    payment.getLoanId(),
                    restoredOutstanding
            );

            audit.save(
                    "PAYMENT_REVERSED",
                    "PAYMENT",
                    payment.getId(),
                    "SYSTEM",
                    "originalPaymentId=" + payment.getId()
                            + "; reversalPaymentId=" + reversalPaymentId
                            + "; loanId=" + payment.getLoanId()
                            + "; installmentId=" + payment.getInstallmentId()
                            + "; amount=" + paymentAmount.toPlainString()
            );
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException("Reverse payment failed: " + exception.getMessage(), exception);
        }
    }

    @Override
    public void undo() {
        // Financial reversals are immutable history; transaction rollback is handled by the service.
    }

    public int getReversalPaymentId() {
        return reversalPaymentId;
    }

    private void validatePayment() throws Exception {
        if (payment == null) {
            throw new IllegalArgumentException("Payment is required");
        }

        if (payment.getAmountDecimal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Original payment amount must be greater than zero");
        }

        if (payment.getReversalOfPaymentId() != null
                || "REVERSAL".equalsIgnoreCase(payment.getStatus())) {
            throw new IllegalStateException("A reversal payment cannot be reversed");
        }

        if ("REVERSED".equalsIgnoreCase(payment.getStatus())
                || payments.findReversalByOriginalPayment(payment.getId()).isPresent()) {
            throw new IllegalStateException("Payment already reversed");
        }

        if (!"COMPLETED".equalsIgnoreCase(payment.getStatus())) {
            throw new IllegalStateException("Only completed original payments can be reversed");
        }
    }

    private String installmentStatusAfterReversal(
            Installment installment,
            BigDecimal restoredPaid
    ) {
        BigDecimal required = installment.getTotalAmount();
        if (required == null || required.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Installment total amount is invalid");
        }

        if (restoredPaid.compareTo(required) >= 0) {
            return "PAID";
        }

        if (restoredPaid.compareTo(BigDecimal.ZERO) > 0) {
            return "PARTIALLY_PAID";
        }

        LocalDate dueDate = installment.getDueDate();
        return dueDate != null && dueDate.isBefore(LocalDate.now())
                ? "OVERDUE"
                : "PENDING";
    }
}
