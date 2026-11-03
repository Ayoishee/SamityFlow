package com.samityflow.repository;

import com.samityflow.model.LedgerValue;
import com.samityflow.model.SavingsTransaction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Persists and queries SavingsTransaction records using JDBC. */
public class SavingsTransactionRepository {

    private final Connection connection;

    public SavingsTransactionRepository(Connection connection) {
        this.connection = connection;
    }

    public void save(int memberId, String type, double amount, String reference)
            throws SQLException {

        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Savings amount must be positive");
        }
        long amountCents = com.samityflow.model.Money.cents(java.math.BigDecimal.valueOf(amount));
        if (amountCents <= 0) {
            throw new IllegalArgumentException("Savings amount rounds to zero");
        }
        amount = com.samityflow.model.Money.decimal(amountCents).doubleValue();

        if (!LedgerValue.DEPOSIT.equals(type) && !LedgerValue.WITHDRAWAL.equals(type)) {
            throw new IllegalArgumentException("Savings type must be DEPOSIT or WITHDRAWAL");
        }
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Savings reference is required");
        }

        try (PreparedStatement preparedStatement =
                connection.prepareStatement(
                        """
                            INSERT INTO savings_transactions
                            (member_id, transaction_type, amount,
                             transaction_reference, date, amount_cents)
                            VALUES(?,?,?,?,CURRENT_TIMESTAMP,?)
                        """)) {

            preparedStatement.setInt(1, memberId);
            preparedStatement.setString(2, type);
            preparedStatement.setDouble(3, amount);
            preparedStatement.setString(4, reference);
            preparedStatement.setLong(
                    5, com.samityflow.model.Money.cents(java.math.BigDecimal.valueOf(amount)));

            preparedStatement.executeUpdate();
        }
    }

    /** Compatibility accessor; authorization must compare balanceCents to integer cents. */
    public double calculateBalance(int memberId) throws SQLException {
        return com.samityflow.model.Money.decimal(balanceCents(memberId)).doubleValue();
    }

    /** Exact signed ledger balance; SUM overflow throws instead of permitting overdraft. */
    public long balanceCents(int memberId) throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        "SELECT COALESCE(SUM(CASE WHEN transaction_type='DEPOSIT' THEN amount_cents ELSE -amount_cents END),0) FROM savings_transactions WHERE member_id=?")) {
            statement.setInt(1, memberId);
            try (ResultSet result = statement.executeQuery()) {
                return result.getLong(1);
            }
        }
    }

    public boolean referenceExists(String reference) throws SQLException {
        try (PreparedStatement preparedStatement =
                connection.prepareStatement(
                        "SELECT COUNT(*) FROM savings_transactions WHERE"
                                + " transaction_reference=?")) {
            preparedStatement.setString(1, reference);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /** Links only the deposit created in the same collection transaction. */
    public void saveLinked(
            int memberId, java.math.BigDecimal amount, String reference, int paymentId)
            throws SQLException {
        save(memberId, LedgerValue.DEPOSIT, amount.doubleValue(), reference);
        try (PreparedStatement statement =
                connection.prepareStatement(
                        "UPDATE savings_transactions SET payment_id=? WHERE"
                                + " transaction_id=last_insert_rowid() AND member_id=(SELECT member_id"
                                + " FROM payments WHERE payment_id=?)")) {
            statement.setInt(1, paymentId);
            statement.setInt(2, paymentId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Payment and savings members differ");
            }
        }
    }

    /** Never infers a relationship from references, including matching legacy references. */
    public boolean reverseDeposit(int paymentId) throws SQLException {
        try (PreparedStatement query =
                connection.prepareStatement(
                        "SELECT transaction_id,member_id,amount_cents FROM savings_transactions"
                                + " WHERE payment_id=? AND transaction_type='DEPOSIT'")) {
            query.setInt(1, paymentId);
            try (ResultSet result = query.executeQuery()) {
                if (!result.next()) {
                    return false;
                }
                int transactionId = result.getInt("transaction_id");
                int memberId = result.getInt("member_id");
                long cents = result.getLong("amount_cents");
                if (balanceCents(memberId) < cents) {
                    throw new IllegalStateException(
                            "Linked savings already spent; reversal requires reconciliation");
                }
                save(
                        memberId,
                        LedgerValue.WITHDRAWAL,
                        com.samityflow.model.Money.decimal(cents).doubleValue(),
                        "REVERSAL-SAVINGS-" + paymentId);
                try (PreparedStatement update =
                        connection.prepareStatement(
                                "UPDATE savings_transactions SET reversal_of_transaction_id=? WHERE"
                                        + " transaction_id=last_insert_rowid()")) {
                    update.setInt(1, transactionId);
                    update.executeUpdate();
                }
                return true;
            }
        }
    }

    public List<SavingsTransaction> findByMember(int memberId) throws SQLException {
        List<SavingsTransaction> result = new ArrayList<>();
        try (PreparedStatement preparedStatement =
                connection.prepareStatement(
                        """
                        SELECT transaction_id, member_id, amount, transaction_type
                        FROM savings_transactions WHERE member_id=? ORDER BY transaction_id DESC
                        """)) {
            preparedStatement.setInt(1, memberId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    result.add(
                            new SavingsTransaction(
                                    rs.getInt("transaction_id"), rs.getInt("member_id"),
                                    rs.getDouble("amount"), rs.getString("transaction_type")));
                }
            }
        }
        return result;
    }
}
