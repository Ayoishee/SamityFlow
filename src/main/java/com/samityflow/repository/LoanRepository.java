package com.samityflow.repository;

import com.samityflow.model.Loan;

import java.math.BigDecimal;
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
            ps.setBigDecimal(3, loan.getAmountDecimal());
            ps.setBigDecimal(4, loan.getOutstandingDecimal());
            ps.setString(5, "PENDING");
            ps.executeUpdate();
        }
    }

    public Optional<Loan> findByApplicationId(int applicationId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT loan_id FROM loans WHERE application_id=?")) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? findById(rs.getInt(1)) : Optional.empty();
            }
        }
    }

    public void updateStatus(int loanId, String status) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE loans SET status=? WHERE loan_id=?")) {
            ps.setString(1, status);
            ps.setInt(2, loanId);
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
                        rs.getBigDecimal("principal").doubleValue()
                );
                loan.setOutstanding(rs.getBigDecimal("outstanding_balance"));
                loan.setStatus(rs.getString("status"));
                return Optional.of(loan);
            }
        }
    }

    public java.util.List<Loan> findAllActive() throws SQLException {
        java.util.List<Loan> result = new java.util.ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM loans WHERE status IN ('ACTIVE','DEFAULTED') ORDER BY loan_id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Loan loan = new Loan(
                        rs.getInt("loan_id"),
                        rs.getInt("application_id"),
                        rs.getInt("member_id"),
                        rs.getBigDecimal("principal").doubleValue()
                );
                loan.setOutstanding(rs.getBigDecimal("outstanding_balance"));
                loan.setStatus(rs.getString("status"));
                result.add(loan);
            }
        }
        return result;
    }

    public void updateOutstandingBalance(int loanId, double amount)
            throws SQLException {
        updateOutstandingBalance(loanId, BigDecimal.valueOf(amount));
    }

    public void updateOutstandingBalance(int loanId, BigDecimal amount)
            throws SQLException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Loan outstanding balance cannot be negative");
        }

        try (PreparedStatement ps = connection.prepareStatement("""
                UPDATE loans
                SET outstanding_balance=?
                WHERE loan_id=?
                """)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, loanId);

            if (ps.executeUpdate() != 1) {
                throw new SQLException("Loan balance update affected an unexpected number of rows");
            }
        }
    }
}
