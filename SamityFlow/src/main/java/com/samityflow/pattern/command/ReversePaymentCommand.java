package com.samityflow.pattern.command;

import com.samityflow.model.Payment;
import com.samityflow.repository.AuditLogRepository;
import com.samityflow.repository.InstallmentRepository;
import com.samityflow.repository.LoanRepository;
import com.samityflow.repository.PaymentRepository;

public class ReversePaymentCommand implements PaymentCommand {

    private final Payment payment;
    private final PaymentRepository payments;
    private final InstallmentRepository installments;
    private final LoanRepository loans;
    private final AuditLogRepository audit;

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

            if (payment == null) {
                throw new IllegalArgumentException("Payment is required");
            }

            if ("REVERSED".equalsIgnoreCase(payment.getStatus())) {
                throw new IllegalStateException(
                        "Payment already reversed"
                );
            }

            if ("REVERSAL".equalsIgnoreCase(payment.getStatus())) {
                throw new IllegalStateException(
                        "A reversal payment cannot be reversed"
                );
            }

            if (payments
                    .findReversalByOriginalPayment(payment.getId())
                    .isPresent()) {

                throw new IllegalStateException(
                        "Reversal already exists for payment "
                                + payment.getId()
                );
            }

            var installment = installments
                    .findById(payment.getInstallmentId())
                    .orElseThrow(
                            () -> new IllegalStateException(
                                    "Installment not found"
                            )
                    );

            var loan = loans
                    .findById(payment.getLoanId())
                    .orElseThrow(
                            () -> new IllegalStateException(
                                    "Loan not found"
                            )
                    );

            double paymentAmount = payment.getAmount();

            if (paymentAmount <= 0) {
                throw new IllegalStateException(
                        "Original payment amount must be greater than zero"
                );
            }

            double restoredPaidAmount =
                    installment.getPaid() - paymentAmount;

            if (restoredPaidAmount < 0) {
                restoredPaidAmount = 0;
            }

            double restoredOutstandingBalance =
                    loan.getOutstanding() + paymentAmount;

            if (restoredOutstandingBalance < 0) {
                throw new IllegalStateException(
                        "Invalid loan outstanding balance"
                );
            }

            /*
             * Keep the original payment.
             * Create a separate negative reversal payment.
             */
            payments.saveReversal(payment);

            /*
             * Mark the original financial record as reversed.
             */
            payments.updateStatus(
                    payment.getId(),
                    "REVERSED"
            );

            /*
             * Restore the installment paid amount.
             */
            installments.updatePaidAmount(
                    payment.getInstallmentId(),
                    restoredPaidAmount
            );

            /*
             * Restore the loan outstanding balance.
             */
            loans.updateOutstandingBalance(
                    payment.getLoanId(),
                    restoredOutstandingBalance
            );

            /*
             * Record the reversal in the audit trail.
             */
            audit.save(
                    "PAYMENT_REVERSAL",
                    "PAYMENT",
                    payment.getId(),
                    "SYSTEM",
                    "Payment "
                            + payment.getId()
                            + " reversed. Original amount: "
                            + paymentAmount
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Reverse payment failed: "
                            + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public void undo() {
        /*
         * Financial history is preserved.
         * A reversal is not deleted or directly undone.
         * Any later correction should be represented
         * by another financial transaction.
         */
    }
}

