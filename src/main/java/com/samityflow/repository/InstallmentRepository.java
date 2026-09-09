
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
        PreparedStatement ps=connection.prepareStatement(
            "SELECT * FROM installments WHERE installment_id=?");
        ps.setInt(1,id);
        ResultSet rs=ps.executeQuery();

        if(!rs.next()) return Optional.empty();

        Installment i=new Installment();
        i.setPaidAmount(BigDecimal.valueOf(rs.getDouble("paid_amount")));
        i.setTotalAmount(BigDecimal.valueOf(rs.getDouble("total_amount")));
        i.setStatus(rs.getString("status"));
        return Optional.of(i);
    }

    public void updatePayment(int id, BigDecimal paid, String status)
            throws SQLException {
        PreparedStatement ps=connection.prepareStatement("""
            UPDATE installments
            SET paid_amount=?, status=?
            WHERE installment_id=?
        """);
        ps.setDouble(1, paid.doubleValue());
        ps.setString(2,status);
        ps.setInt(3,id);
        ps.executeUpdate();
    }

    public List<Installment> findDueInstallments(int samityId)
            throws SQLException {
        return new ArrayList<>();
    }
}
