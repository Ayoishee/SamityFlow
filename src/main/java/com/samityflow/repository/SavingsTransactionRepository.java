package com.samityflow.repository;

import com.samityflow.model.SavingsTransaction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SavingsTransactionRepository {

    private final Connection connection;

    public SavingsTransactionRepository(Connection connection) {
        this.connection = connection;
    }

    public void save(int memberId, String type,
                     double amount, String reference)
            throws SQLException {

        if (amount <= 0) throw new IllegalArgumentException("Savings amount must be positive");
        if (!"DEPOSIT".equals(type) && !"WITHDRAWAL".equals(type)) {
            throw new IllegalArgumentException("Savings type must be DEPOSIT or WITHDRAWAL");
        }
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Savings reference is required");
        }

        try (PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO savings_transactions
            (member_id, transaction_type, amount,
             transaction_reference, date)
            VALUES(?,?,?,?,CURRENT_TIMESTAMP)
        """)) {

            ps.setInt(1, memberId);
            ps.setString(2, type);
            ps.setDouble(3, amount);
            ps.setString(4, reference);

            ps.executeUpdate();
        }
    }

    public double calculateBalance(int memberId)
            throws SQLException {

        try (PreparedStatement ps = connection.prepareStatement("""
            SELECT SUM(
                CASE WHEN transaction_type='DEPOSIT'
                THEN amount ELSE -amount END
            )
            FROM savings_transactions
            WHERE member_id=?
        """)) {

            ps.setInt(1, memberId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        }
    }

    public boolean referenceExists(String reference) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM savings_transactions WHERE transaction_reference=?")) {
            ps.setString(1, reference);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean reverseDeposit(String originalReference, int paymentId) throws SQLException {
        String lookup = """
                SELECT member_id, amount FROM savings_transactions
                WHERE transaction_reference=? AND transaction_type='DEPOSIT'
                """;
        int memberId;
        double amount;
        try (PreparedStatement ps = connection.prepareStatement(lookup)) {
            ps.setString(1, originalReference);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                memberId = rs.getInt("member_id");
                amount = rs.getDouble("amount");
            }
        }
        save(memberId, "WITHDRAWAL", amount, "REVERSAL-SAVINGS-" + paymentId);
        return true;
    }

    public List<SavingsTransaction> findByMember(int memberId) throws SQLException {
        List<SavingsTransaction> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT transaction_id, member_id, amount, transaction_type
                FROM savings_transactions WHERE member_id=? ORDER BY transaction_id DESC
                """)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new SavingsTransaction(
                            rs.getInt("transaction_id"), rs.getInt("member_id"),
                            rs.getDouble("amount"), rs.getString("transaction_type")));
                }
            }
        }
        return result;
    }
}
