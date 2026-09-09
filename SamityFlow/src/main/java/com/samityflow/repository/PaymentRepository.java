package com.samityflow.repository;


import com.samityflow.model.Payment;

import java.sql.*;
import java.util.Optional;



public class PaymentRepository {


    private final Connection connection;



    public PaymentRepository(Connection connection){

        this.connection = connection;

    }





    public void save(
            int installmentId,
            int loanId,
            int memberId,
            double amount,
            String reference
    ) throws SQLException {


        PreparedStatement ps =
                connection.prepareStatement("""
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






    public Optional<Payment> findById(
            int id
    ) throws SQLException {


        PreparedStatement ps =
                connection.prepareStatement("""
                SELECT *
                FROM payments
                WHERE payment_id=?
                """);


        ps.setInt(1,id);


        ResultSet rs =
                ps.executeQuery();



        if(!rs.next()){

            return Optional.empty();

        }



        Payment payment = new Payment();


        payment.setId(
                rs.getInt("payment_id")
        );


        payment.setLoanId(
                rs.getInt("loan_id")
        );


        payment.setInstallmentId(
                rs.getInt("installment_id")
        );


        payment.setMemberId(
                rs.getInt("member_id")
        );


        payment.setAmount(
                rs.getDouble("amount")
        );


        payment.setReference(
                rs.getString("payment_reference")
        );


        payment.setStatus(
                rs.getString("status")
        );



        return Optional.of(payment);

    }






    public void updateStatus(
            int id,
            String status
    ) throws SQLException {


        PreparedStatement ps =
                connection.prepareStatement("""
                UPDATE payments
                SET status=?
                WHERE payment_id=?
                """);


        ps.setString(1,status);

        ps.setInt(2,id);


        ps.executeUpdate();

    }






    public void saveReversal(
            Payment payment
    ) throws SQLException {


        PreparedStatement ps =
                connection.prepareStatement("""
                INSERT INTO payments(
                    installment_id,
                    loan_id,
                    member_id,
                    amount,
                    payment_reference,
                    payment_date,
                    status
                )
                VALUES(?,?,?,?,?,CURRENT_TIMESTAMP,'REVERSAL')
                """);


        ps.setInt(1,payment.getInstallmentId());

        ps.setInt(2,payment.getLoanId());

        ps.setInt(3,payment.getMemberId());

        ps.setDouble(4,-payment.getAmount());

        ps.setString(
                5,
                "REVERSAL-"+payment.getId()
        );


        ps.executeUpdate();

    }


}