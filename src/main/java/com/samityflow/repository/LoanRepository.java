package com.samityflow.repository;

import com.samityflow.model.LedgerValue;
import com.samityflow.model.Loan;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/** Persists and queries Loan records using JDBC. */
public class LoanRepository {

    private final Connection connection;

    public LoanRepository(Connection connection) {
        this.connection = connection;
    }

    public void save(Loan loan) throws SQLException {
        String sql =
                """
                INSERT INTO loans(
                    application_id,
                    member_id,
                    principal,
                    outstanding_balance,
                    status
                )
                VALUES(?,?,?,?,?)
                """;

        try (PreparedStatement preparedStatement =
                connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, loan.getApplicationId());
            preparedStatement.setInt(2, loan.getMemberId());
            preparedStatement.setBigDecimal(3, loan.getAmountDecimal());
            preparedStatement.setBigDecimal(4, loan.getOutstandingDecimal());
            preparedStatement.setString(5, LedgerValue.PENDING);
            preparedStatement.executeUpdate();
        }
    }

    public Optional<Loan> findByApplicationId(int applicationId) throws SQLException {
        try (PreparedStatement preparedStatement =
                connection.prepareStatement("SELECT loan_id FROM loans WHERE application_id=?")) {
            preparedStatement.setInt(1, applicationId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                return rs.next() ? findById(rs.getInt(1)) : Optional.empty();
            }
        }
    }

    public void updateStatus(int loanId, String status) throws SQLException {
        try (PreparedStatement preparedStatement =
                connection.prepareStatement("UPDATE loans SET status=? WHERE loan_id=?")) {
            preparedStatement.setString(1, status);
            preparedStatement.setInt(2, loanId);
            preparedStatement.executeUpdate();
        }
    }

    public Optional<Loan> findById(int id) throws SQLException {
        try (PreparedStatement preparedStatement =
                connection.prepareStatement("SELECT * FROM loans WHERE loan_id=?")) {
            preparedStatement.setInt(1, id);

            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                Loan loan =
                        new Loan(
                                rs.getInt("loan_id"),
                                rs.getInt("application_id"),
                                rs.getInt("member_id"),
                                com.samityflow.model.Money.round(rs.getBigDecimal("principal"))
                                        .doubleValue());
                loan.setOutstanding(
                        com.samityflow.model.Money.round(rs.getBigDecimal("outstanding_balance")));
                loan.setStatus(rs.getString("status"));
                return Optional.of(loan);
            }
        }
    }

    public java.util.List<Loan> findAllActive() throws SQLException {
        java.util.List<Loan> result = new java.util.ArrayList<>();
        try (PreparedStatement preparedStatement =
                        connection.prepareStatement(
                                "SELECT * FROM loans WHERE status IN ('ACTIVE','DEFAULTED') ORDER"
                                        + " BY loan_id DESC");
                ResultSet rs = preparedStatement.executeQuery()) {
            while (rs.next()) {
                Loan loan =
                        new Loan(
                                rs.getInt("loan_id"),
                                rs.getInt("application_id"),
                                rs.getInt("member_id"),
                                com.samityflow.model.Money.round(rs.getBigDecimal("principal"))
                                        .doubleValue());
                loan.setOutstanding(
                        com.samityflow.model.Money.round(rs.getBigDecimal("outstanding_balance")));
                loan.setStatus(rs.getString("status"));
                result.add(loan);
            }
        }
        return result;
    }

    public void updateOutstandingBalance(int loanId, double amount) throws SQLException {
        updateOutstandingBalance(loanId, BigDecimal.valueOf(amount));
    }

    public void updateOutstandingBalance(int loanId, BigDecimal amount) throws SQLException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Loan outstanding balance cannot be negative");
        }

        try (PreparedStatement preparedStatement =
                connection.prepareStatement(
                        """
                        UPDATE loans
                        SET outstanding_balance=?
                        WHERE loan_id=?
                        """)) {
            preparedStatement.setBigDecimal(1, amount);
            preparedStatement.setInt(2, loanId);

            if (preparedStatement.executeUpdate() != 1) {
                throw new SQLException("Loan balance update affected an unexpected number of rows");
            }
        }
    }
}
