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

        PreparedStatement ps = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
        );

        ps.setInt(1, loan.getApplicationId());
        ps.setInt(2, loan.getMemberId());
        ps.setDouble(3, loan.getAmount());
        ps.setDouble(4, loan.getOutstanding());
        ps.setString(5, "PENDING");

        ps.executeUpdate();
    }

    public Optional<Loan> findById(int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM loans WHERE loan_id=?"
        );

        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return Optional.empty(); // map with existing Loan constructor
        }

        return Optional.empty();
    }

    public void updateOutstandingBalance(int loanId, double amount)
            throws SQLException {

        PreparedStatement ps = connection.prepareStatement(
                """
                UPDATE loans
                SET outstanding_balance=?
                WHERE loan_id=?
                """
        );

        ps.setDouble(1, amount);
        ps.setInt(2, loanId);

        ps.executeUpdate();
    }
}
