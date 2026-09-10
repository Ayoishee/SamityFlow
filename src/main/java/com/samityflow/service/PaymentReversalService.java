package com.samityflow.service;

import com.samityflow.database.Database;
import com.samityflow.model.Payment;
import com.samityflow.pattern.command.ReversePaymentCommand;
import com.samityflow.repository.AuditLogRepository;
import com.samityflow.repository.InstallmentRepository;
import com.samityflow.repository.LoanRepository;
import com.samityflow.repository.PaymentRepository;
import com.samityflow.repository.SavingsTransactionRepository;

import java.sql.Connection;

public class PaymentReversalService {

    private final Database database;

    public PaymentReversalService() {
        this(Database.applicationDatabase());
    }

    public PaymentReversalService(Database database) {
        this.database = database;
    }

    public void reversePayment(int paymentId) {
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);

            try {
                PaymentRepository payments = new PaymentRepository(connection);
                Payment originalPayment = payments
                        .findById(paymentId)
                        .orElseThrow(() -> new IllegalStateException("Payment not found"));

                ReversePaymentCommand command = new ReversePaymentCommand(
                        originalPayment,
                        payments,
                        new InstallmentRepository(connection),
                        new LoanRepository(connection),
                        new AuditLogRepository(connection)
                );

                command.execute();
                new SavingsTransactionRepository(connection)
                        .reverseDeposit(originalPayment.getReference(), paymentId);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new RuntimeException("Payment reversal failed", exception);
            }
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException("Payment reversal failed", exception);
        }
    }
}
