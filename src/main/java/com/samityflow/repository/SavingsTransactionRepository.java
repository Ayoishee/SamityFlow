package com.samityflow.repository;

import java.sql.*;

public class SavingsTransactionRepository {

    private final Connection connection;

    public SavingsTransactionRepository(Connection connection) {
        this.connection = connection;
    }

    public void save(int memberId, String type,
                     double amount, String reference)
            throws SQLException {

        PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO savings_transactions
            (member_id, transaction_type, amount,
             transaction_reference, date)
            VALUES(?,?,?,?,CURRENT_TIMESTAMP)
        """);

        ps.setInt(1, memberId);
        ps.setString(2, type);
        ps.setDouble(3, amount);
        ps.setString(4, reference);

        ps.executeUpdate();
    }

    public double calculateBalance(int memberId)
            throws SQLException {

        PreparedStatement ps = connection.prepareStatement("""
            SELECT SUM(
                CASE WHEN transaction_type='DEPOSIT'
                THEN amount ELSE -amount END
            )
            FROM savings_transactions
            WHERE member_id=?
        """);

        ps.setInt(1, memberId);

        ResultSet rs = ps.executeQuery();

        return rs.next() ? rs.getDouble(1) : 0;
    }
}
