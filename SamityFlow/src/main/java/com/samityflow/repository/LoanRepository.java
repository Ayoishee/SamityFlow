package com.samityflow.repository;


import com.samityflow.model.Loan;

import java.sql.*;
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



        PreparedStatement ps =
                connection.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS
                );



        ps.setInt(
                1,
                loan.getApplicationId()
        );


        ps.setInt(
                2,
                loan.getMemberId()
        );


        ps.setDouble(
                3,
                loan.getAmount()
        );


        ps.setDouble(
                4,
                loan.getOutstanding()
        );


        ps.setString(
                5,
                "PENDING"
        );


        ps.executeUpdate();

    }





    public Optional<Loan> findById(
            int id
    ) throws SQLException {


        PreparedStatement ps =
                connection.prepareStatement(
                        "SELECT * FROM loans WHERE loan_id=?"
                );


        ps.setInt(
                1,
                id
        );


        ResultSet rs =
                ps.executeQuery();



        if(rs.next()) {


            return Optional.of(
                    mapLoan(rs)
            );


        }



        return Optional.empty();

    }






    public Loan findByApplicationId(
            int applicationId
    )
            throws SQLException {



        PreparedStatement ps =
                connection.prepareStatement(
                        """
                        SELECT *
                        FROM loans
                        WHERE application_id=?
                        """
                );



        ps.setInt(
                1,
                applicationId
        );



        ResultSet rs =
                ps.executeQuery();



        if(rs.next()) {

            return mapLoan(rs);

        }



        return null;

    }






    public void updateStatus(
            int loanId,
            String status
    )
            throws SQLException {



        PreparedStatement ps =
                connection.prepareStatement(
                        """
                        UPDATE loans
                        SET status=?
                        WHERE loan_id=?
                        """
                );



        ps.setString(
                1,
                status
        );


        ps.setInt(
                2,
                loanId
        );


        ps.executeUpdate();

    }







    public void updateOutstandingBalance(
            int loanId,
            double amount
    )
            throws SQLException {



        PreparedStatement ps =
                connection.prepareStatement(
                        """
                        UPDATE loans
                        SET outstanding_balance=?
                        WHERE loan_id=?
                        """
                );



        ps.setDouble(
                1,
                amount
        );


        ps.setInt(
                2,
                loanId
        );



        ps.executeUpdate();

    }








    public void increaseOutstandingBalance(
            int loanId,
            double amount
    )
            throws SQLException {



        PreparedStatement ps =
                connection.prepareStatement(
                        """
                        UPDATE loans
                        SET outstanding_balance =
                        outstanding_balance + ?
                        WHERE loan_id=?
                        """
                );



        ps.setDouble(
                1,
                amount
        );


        ps.setInt(
                2,
                loanId
        );


        ps.executeUpdate();

    }







    private Loan mapLoan(
            ResultSet rs
    )
            throws SQLException {



        Loan loan = new Loan();



        loan.setId(
                rs.getInt("loan_id")
        );


        loan.setApplicationId(
                rs.getInt("application_id")
        );


        loan.setMemberId(
                rs.getInt("member_id")
        );


        loan.setAmount(
                rs.getDouble("principal")
        );


        loan.setOutstanding(
                rs.getDouble(
                        "outstanding_balance"
                )
        );



        return loan;

    }


}