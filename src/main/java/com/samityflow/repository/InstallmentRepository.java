package com.samityflow.repository;

import com.samityflow.model.Installment;

import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class InstallmentRepository {

    private final Connection connection;

    public InstallmentRepository(Connection connection){
        this.connection = connection;
    }


    public Optional<Installment> findById(int id) throws SQLException {

        PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM installments WHERE installment_id=?"
        );

        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if(!rs.next()) {
            return Optional.empty();
        }


        Installment i = new Installment();

        i.setId(rs.getInt("installment_id"));
        i.setLoanId(rs.getInt("loan_id"));
        i.setNumber(rs.getInt("installment_number"));

        i.setDueDate(
                LocalDate.parse(rs.getString("due_date"))
        );

        i.setPrincipalAmount(
                rs.getBigDecimal("principal_amount")
        );

        i.setInterestAmount(
                rs.getBigDecimal("interest_amount")
        );

        i.setTotalAmount(
                rs.getBigDecimal("total_amount")
        );

        i.setPenaltyAmount(
                rs.getBigDecimal("penalty_amount")
        );

        i.setPaidAmount(
                rs.getBigDecimal("paid_amount")
        );

        i.setStatus(
                rs.getString("status")
        );


        return Optional.of(i);
    }



    public void updatePayment(
            int id,
            BigDecimal paid,
            String status
    ) throws SQLException {


        PreparedStatement ps = connection.prepareStatement("""
            UPDATE installments
            SET 
                paid_amount=?,
                status=?
            WHERE installment_id=?
        """);


        ps.setBigDecimal(1, paid);
        ps.setString(2, status);
        ps.setInt(3, id);

        ps.executeUpdate();
    }



    public void saveAll(List<Installment> items)
            throws SQLException {

        String sql = """
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


        try(PreparedStatement ps =
                    connection.prepareStatement(sql)) {


            for(Installment i : items){

                ps.setInt(1, i.getLoanId());
                ps.setInt(2, i.getNumber());
                ps.setString(3,
                        i.getDueDate().toString());

                ps.setBigDecimal(4,
                        i.getPrincipalAmount());

                ps.setBigDecimal(5,
                        i.getInterestAmount());

                ps.setBigDecimal(6,
                        i.getTotalAmount());

                ps.setBigDecimal(7,
                        i.getPenaltyAmount());

                ps.setBigDecimal(8,
                        i.getPaidAmount());

                ps.setString(9,
                        i.getStatus());

                ps.addBatch();
            }

            ps.executeBatch();
        }
    }


    public List<Installment> findDueInstallments(int samityId)
            throws SQLException {
        String sql = """
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
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, samityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        }
        return result;
    }

    public List<Installment> findOutstanding() throws SQLException {
        List<Installment> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT * FROM installments
                WHERE status IN ('PENDING','PARTIALLY_PAID','OVERDUE')
                ORDER BY due_date, installment_number
                """); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(map(rs));
        }
        return result;
    }

    public void updatePenaltyAndStatus(int id, BigDecimal penalty, String status)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                UPDATE installments SET penalty_amount=?, status=? WHERE installment_id=?
                """)) {
            ps.setBigDecimal(1, penalty);
            ps.setString(2, status);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    private Installment map(ResultSet rs) throws SQLException {
        Installment item = new Installment();
        item.setId(rs.getInt("installment_id"));
        item.setLoanId(rs.getInt("loan_id"));
        item.setNumber(rs.getInt("installment_number"));
        item.setDueDate(LocalDate.parse(rs.getString("due_date")));
        item.setPrincipalAmount(rs.getBigDecimal("principal_amount"));
        item.setInterestAmount(rs.getBigDecimal("interest_amount"));
        item.setTotalAmount(rs.getBigDecimal("total_amount"));
        item.setPenaltyAmount(rs.getBigDecimal("penalty_amount"));
        item.setPaidAmount(rs.getBigDecimal("paid_amount"));
        item.setStatus(rs.getString("status"));
        return item;
    }
}
