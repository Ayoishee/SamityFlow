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

            BigDecimal paymentAmount = BigDecimal.valueOf(payment.getAmount());
            BigDecimal currentPaid = installment.getPaidAmount();
            BigDecimal restoredPaid = currentPaid.subtract(paymentAmount);

            if (restoredPaid.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException(
                        "Reversal would make installment paid amount negative"
                );
            }

            BigDecimal currentOutstanding = BigDecimal.valueOf(loan.getOutstanding());
            if (currentOutstanding.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("Loan outstanding balance is invalid");
            }

            BigDecimal restoredOutstanding = currentOutstanding.add(paymentAmount);
            if (restoredOutstanding.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("Restored loan outstanding balance is invalid");
            }

            String restoredStatus = installmentStatusAfterReversal(
                    installment,
                    restoredPaid
            );

            reversalPaymentId = payments.saveReversal(payment);
            payments.updateStatus(payment.getId(), "REVERSED");
            installments.updatePayment(
                    payment.getInstallmentId(),
                    restoredPaid,
                    restoredStatus
            );
            loans.updateOutstandingBalance(
                    payment.getLoanId(),
                    restoredOutstanding.doubleValue()
            );

            audit.save(
                    "PAYMENT_REVERSAL",
                    "PAYMENT",
                    payment.getId(),
                    "SYSTEM",
                    "Original payment " + payment.getId()
                            + " reversed by payment " + reversalPaymentId
                            + "; amount=" + paymentAmount.toPlainString()
                            + "; installment=" + payment.getInstallmentId()
                            + "; loan=" + payment.getLoanId()
            );
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException("Reverse payment failed: " + exception.getMessage(), exception);
        }
    }

    @Override
    public void undo() {
        // Reversal records are financial history and are not deleted by undo.
    }

    public int getReversalPaymentId() {
        return reversalPaymentId;
    }

    private void validatePayment() throws Exception {
        if (payment == null) {
            throw new IllegalArgumentException("Payment is required");
        }

        if (payment.getAmount() <= 0) {
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

        if ("OVERDUE".equalsIgnoreCase(installment.getStatus())) {
            return "OVERDUE";
        }

        LocalDate dueDate = installment.getDueDate();
        return dueDate != null && dueDate.isBefore(LocalDate.now())
                ? "OVERDUE"
                : "PENDING";
    }
}
