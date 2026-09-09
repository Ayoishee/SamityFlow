package com.samityflow.repository;

import com.samityflow.model.Loan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class LoanRepository {

    private final Connection connection;

    public LoanRepository(Connection connection) {
        this.connection = connection;
    }

    public void save(Loan loan) throws SQLException {
        String sql = """
                INSERT INTO loans(
                    application_id,
                    member_id,
                    principal,
                    outstanding_balance,
                    status
                )
                VALUES(?,?,?,?,?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
        )) {
            ps.setInt(1, loan.getApplicationId());
            ps.setInt(2, loan.getMemberId());
            ps.setDouble(3, loan.getAmount());
            ps.setDouble(4, loan.getOutstanding());
            ps.setString(5, "PENDING");
            ps.executeUpdate();
        }
    }

    public Optional<Loan> findById(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM loans WHERE loan_id=?")) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                Loan loan = new Loan(
                        rs.getInt("loan_id"),
                        rs.getInt("application_id"),
                        rs.getInt("member_id"),
                        rs.getDouble("principal")
                );
                loan.setOutstanding(rs.getDouble("outstanding_balance"));
                return Optional.of(loan);
            }
        }
    }

    public void updateOutstandingBalance(int loanId, double amount)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                UPDATE loans
                SET outstanding_balance=?
                WHERE loan_id=?
                """)) {
            ps.setDouble(1, amount);
            ps.setInt(2, loanId);

            if (ps.executeUpdate() != 1) {
                throw new SQLException("Loan balance update affected an unexpected number of rows");
            }
        }
    }
}
