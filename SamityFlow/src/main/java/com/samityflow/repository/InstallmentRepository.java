package com.samityflow.repository;


import com.samityflow.model.Installment;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;



public class InstallmentRepository {


    private final Connection connection;



    public InstallmentRepository(Connection connection){

        this.connection = connection;

    }





    public Optional<Installment> findById(
            int id
    ) throws SQLException {


        PreparedStatement ps =
                connection.prepareStatement("""
                SELECT *
                FROM installments
                WHERE installment_id=?
                """);


        ps.setInt(1,id);


        ResultSet rs =
                ps.executeQuery();



        if(!rs.next()){

            return Optional.empty();

        }



        Installment installment =
                new Installment();



        installment.setId(
                rs.getInt("installment_id")
        );


        installment.setLoanId(
                rs.getInt("loan_id")
        );


        installment.setPaidAmount(
                rs.getBigDecimal("paid_amount")
        );


        installment.setStatus(
                rs.getString("status")
        );



        return Optional.of(installment);

    }







    public void updatePaidAmount(
            int installmentId,
            BigDecimal amount
    ) throws SQLException {



        PreparedStatement ps =
                connection.prepareStatement("""
                UPDATE installments
                SET paid_amount=?
                WHERE installment_id=?
                """);


        ps.setBigDecimal(1,amount);

        ps.setInt(2,installmentId);


        ps.executeUpdate();

    }







    public void updateStatus(
            int installmentId,
            String status
    ) throws SQLException {



        PreparedStatement ps =
                connection.prepareStatement("""
                UPDATE installments
                SET status=?
                WHERE installment_id=?
                """);


        ps.setString(1,status);

        ps.setInt(2,installmentId);


        ps.executeUpdate();

    }







    public void updatePayment(
            int installmentId,
            BigDecimal paidAmount,
            String status
    ) throws SQLException {



        PreparedStatement ps =
                connection.prepareStatement("""
                UPDATE installments
                SET
                    paid_amount=?,
                    status=?
                WHERE installment_id=?
                """);



        ps.setBigDecimal(
                1,
                paidAmount
        );


        ps.setString(
                2,
                status
        );


        ps.setInt(
                3,
                installmentId
        );



        ps.executeUpdate();

    }

}