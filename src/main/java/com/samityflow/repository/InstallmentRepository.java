package com.samityflow.repository;

import com.samityflow.model.Installment;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Persists and queries Installment records using JDBC. */
public class InstallmentRepository {

    private final Connection connection;

    public InstallmentRepository(Connection connection) {
        this.connection = connection;
    }

    public Optional<Installment> findById(int id) throws SQLException {

        try (PreparedStatement statement =
                connection.prepareStatement("SELECT * FROM installments WHERE installment_id=?")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        }
    }

    public void updatePayment(int id, BigDecimal paid, String status) throws SQLException {

        try (PreparedStatement statement =
                connection.prepareStatement(
                        "UPDATE installments SET paid_amount=?,status=? WHERE installment_id=?")) {
            statement.setBigDecimal(1, paid);
            statement.setString(2, status);
            statement.setInt(3, id);
            statement.executeUpdate();
        }
    }

    public void saveAll(List<Installment> items) throws SQLException {

        String sql =
                """
                    INSERT INTO installments
                    (loan_id,
                     installment_number,
                     due_date,
                     principal_amount,
                     interest_amount,
                     total_amount,
                     penalty_amount,
                     paid_amount,
                     status)
                    VALUES (?,?,?,?,?,?,?,?,?)
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            for (Installment i : items) {

                preparedStatement.setInt(1, i.getLoanId());
                preparedStatement.setInt(2, i.getNumber());
                preparedStatement.setString(3, i.getDueDate().toString());

                preparedStatement.setBigDecimal(4, i.getPrincipalAmount());

                preparedStatement.setBigDecimal(5, i.getInterestAmount());

                preparedStatement.setBigDecimal(6, i.getTotalAmount());

                preparedStatement.setBigDecimal(7, i.getPenaltyAmount());

                preparedStatement.setBigDecimal(8, i.getPaidAmount());

                preparedStatement.setString(9, i.getStatus());

                preparedStatement.addBatch();
            }

            preparedStatement.executeBatch();
        }
    }

    public List<Installment> findDueInstallments(int samityId) throws SQLException {
        String sql =
                """
                SELECT i.*
                FROM installments i
                JOIN loans l ON l.loan_id=i.loan_id
                JOIN members m ON m.id=l.member_id
                JOIN group_units g ON g.id=m.group_unit_id
                WHERE g.samity_id=?
                  AND i.status IN ('PENDING','PARTIALLY_PAID','OVERDUE')
                ORDER BY i.due_date, i.installment_number
                """;
        List<Installment> result = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, samityId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    public List<Installment> findOutstanding() throws SQLException {
        List<Installment> result = new ArrayList<>();
        try (PreparedStatement preparedStatement =
                        connection.prepareStatement(
                                """
                                SELECT * FROM installments
                                WHERE status IN ('PENDING','PARTIALLY_PAID','OVERDUE')
                                ORDER BY due_date, installment_number
                                """);
                ResultSet rs = preparedStatement.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
        }
        return result;
    }

    public void updatePenaltyAndStatus(int id, BigDecimal penalty, String status)
            throws SQLException {
        try (PreparedStatement preparedStatement =
                connection.prepareStatement(
                        """
                        UPDATE installments SET penalty_amount=?, status=? WHERE installment_id=?
                        """)) {
            preparedStatement.setBigDecimal(1, penalty);
            preparedStatement.setString(2, status);
            preparedStatement.setInt(3, id);
            preparedStatement.executeUpdate();
        }
    }

    private Installment map(ResultSet rs) throws SQLException {
        Installment item = new Installment();
        item.setId(rs.getInt("installment_id"));
        item.setLoanId(rs.getInt("loan_id"));
        item.setNumber(rs.getInt("installment_number"));
        item.setDueDate(LocalDate.parse(rs.getString("due_date")));
        item.setPrincipalAmount(
                com.samityflow.model.Money.round(rs.getBigDecimal("principal_amount")));
        item.setInterestAmount(
                com.samityflow.model.Money.round(rs.getBigDecimal("interest_amount")));
        item.setTotalAmount(com.samityflow.model.Money.round(rs.getBigDecimal("total_amount")));
        item.setPenaltyAmount(com.samityflow.model.Money.round(rs.getBigDecimal("penalty_amount")));
        item.setPaidAmount(com.samityflow.model.Money.round(rs.getBigDecimal("paid_amount")));
        item.setStatus(rs.getString("status"));
        return item;
    }
}
