package com.samityflow.service;

import com.samityflow.database.Database;
import com.samityflow.repository.AuditLogRepository;
import com.samityflow.repository.SavingsTransactionRepository;

import java.sql.Connection;

public class SavingsService {
    private final Database database;

    public SavingsService() {
        this(Database.applicationDatabase());
    }

    public SavingsService(Database database) {
        this.database = database;
    }

    public boolean validateWithdrawal(double balance, double amount) {
        return amount > 0 && amount <= balance;
    }

    public double balance(int memberId) {
        try (Connection connection = database.connect()) {
            return new SavingsTransactionRepository(connection).calculateBalance(memberId);
        } catch (Exception exception) {
            throw new RuntimeException("Could not calculate savings balance", exception);
        }
    }

    public void deposit(int memberId, double amount, String reference) {
        save(memberId, "DEPOSIT", amount, reference);
    }

    public void withdraw(int memberId, double amount, String reference) {
        if (!validateWithdrawal(balance(memberId), amount)) {
            throw new IllegalArgumentException("Withdrawal exceeds available savings or is not positive");
        }
        save(memberId, "WITHDRAWAL", amount, reference);
    }

    private void save(int memberId, String type, double amount, String reference) {
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                SavingsTransactionRepository savings = new SavingsTransactionRepository(connection);
                if (savings.referenceExists(reference)) {
                    throw new IllegalArgumentException("Savings reference already used");
                }
                savings.save(memberId, type, amount, reference);
                new AuditLogRepository(connection).save(
                        "SAVINGS_" + type, "MEMBER", memberId, "SYSTEM",
                        type + " amount " + amount + " with reference " + reference);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof RuntimeException runtimeException) throw runtimeException;
                throw new RuntimeException(exception);
            }
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException("Could not save savings transaction", exception);
        }
    }
}
