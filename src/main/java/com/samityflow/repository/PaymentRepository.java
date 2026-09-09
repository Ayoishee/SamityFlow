package com.samityflow.repository;

import java.sql.*;

public class PaymentRepository {

    private final Connection connection;

    public PaymentRepository(Connection connection) {
        this.connection = connection;
    }

    public void save(int installmentId, int loanId, int memberId,
                     double amount, String reference) throws SQLException {

        PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO payments(
                installment_id,
                loan_id,
                member_id,
                amount,
                payment_reference,
                payment_date,
                status
            )
            VALUES(?,?,?,?,?,CURRENT_TIMESTAMP,'COMPLETED')
        """);

        ps.setInt(1, installmentId);
        ps.setInt(2, loanId);
        ps.setInt(3, memberId);
        ps.setDouble(4, amount);
        ps.setString(5, reference);

        ps.executeUpdate();
    }
}
