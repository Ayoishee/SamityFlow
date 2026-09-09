package com.samityflow.repository;


import com.samityflow.model.Installment;

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
                connection.prepareStatement(
                        """
                        SELECT *
                        FROM installments
                        WHERE installment_id=?
                        """
                );


        ps.setInt(1,id);


        ResultSet rs =
                ps.executeQuery();



        if(rs.next()){


            Installment installment =
                    new Installment(
                            rs.getInt("installment_id"),
                            rs.getInt("loan_id"),
                            rs.getInt("installment_number"),
                            rs.getDouble("amount")
                    );



            installment.setPaid(
                    rs.getDouble("paid_amount")
            );


            return Optional.of(installment);

        }


        return Optional.empty();

    }






    public void updatePaidAmount(
            int installmentId,
            double amount
    ) throws SQLException {


        PreparedStatement ps =
                connection.prepareStatement(
                        """
                        UPDATE installments
                        SET paid_amount=?
                        WHERE installment_id=?
                        """
                );


        ps.setDouble(1,amount);

        ps.setInt(2,installmentId);


        ps.executeUpdate();

    }


    public void updatePayment(
            int installmentId,
            double paidAmount
    ) throws SQLException {


        updatePaidAmount(
                installmentId,
                paidAmount
        );

    }


}