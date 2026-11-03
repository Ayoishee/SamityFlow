package com.samityflow.service;

import com.samityflow.database.Database;
import com.samityflow.model.LedgerValue;
import com.samityflow.repository.AuditLogRepository;
import com.samityflow.repository.SavingsTransactionRepository;
import java.sql.Connection;

/** Records member deposits and serializes balance-checked SQLite withdrawals. */
public class SavingsService {
    private final Database database;

    public SavingsService() {
        this(Database.applicationDatabase());
    }

    public SavingsService(Database database) {
        this.database = database;
    }

    /**
     * Checks positive amount against a supplied balance; persistence additionally rechecks under a
     * SQLite write lock.
     */
    public boolean validateWithdrawal(double balance, double amount) {
        return amount > 0 && amount <= balance;
    }

    /**
     * Returns the signed sum of exact savings cents for the member, using a short-lived owned
     * connection. Database failures are wrapped in RuntimeException.
     */
    public double balance(int memberId) {
        try (Connection connection = database.connect()) {
            return new SavingsTransactionRepository(connection).calculateBalance(memberId);
        } catch (Exception exception) {
            throw new RuntimeException("Could not calculate savings balance", exception);
        }
    }

    /**
     * Records a positive deposit with a unique reference and audit entry in an owned transaction.
     * Invalid amounts or references throw IllegalArgumentException.
     */
    public void deposit(int memberId, double amount, String reference) {
        save(memberId, LedgerValue.DEPOSIT, amount, reference);
    }

    /**
     * Atomically locks the SQLite writer, verifies available cents, and records withdrawal and
     * audit. Insufficient funds throw IllegalArgumentException; no partial write survives failure.
     * Uses its own connection.
     */
    public void withdraw(int memberId, double amount, String reference) {
        save(memberId, LedgerValue.WITHDRAWAL, amount, reference);
    }

    private void save(int memberId, String type, double amount, String reference) {
        amount =
                com.samityflow.model.Money.round(java.math.BigDecimal.valueOf(amount))
                        .doubleValue();
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                try (var lock = connection.createStatement()) {
                    lock.executeUpdate("UPDATE members SET id=id WHERE id=" + memberId);
                }
                SavingsTransactionRepository savings = new SavingsTransactionRepository(connection);
                if (LedgerValue.WITHDRAWAL.equals(type)
                        && (amount <= 0
                                || com.samityflow.model.Money.cents(
                                                java.math.BigDecimal.valueOf(amount))
                                        > savings.balanceCents(memberId))) {
                    throw new IllegalArgumentException(
                            "Withdrawal exceeds available savings or is not positive");
                }
                if (savings.referenceExists(reference)) {
                    throw new IllegalArgumentException("Savings reference already used");
                }
                savings.save(memberId, type, amount, reference);
                new AuditLogRepository(connection)
                        .save(
                                "SAVINGS_" + type,
                                "MEMBER",
                                memberId,
                                "SYSTEM",
                                type + " amount " + amount + " with reference " + reference);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new RuntimeException(exception);
            }
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException("Could not save savings transaction", exception);
        }
    }
}
