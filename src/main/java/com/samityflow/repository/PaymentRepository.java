package com.samityflow.repository;

import com.samityflow.model.Payment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Optional;

public class PaymentRepository {

    private final Connection connection;

    public PaymentRepository(Connection connection) {
        this.connection = connection;
    }

    public void save(int installmentId, int loanId, int memberId,
                     double amount, String reference) throws SQLException {
        save(installmentId, loanId, memberId, BigDecimal.valueOf(amount), reference);
    }

    public void save(int installmentId, int loanId, int memberId,
                     BigDecimal amount, String reference) throws SQLException {
        saveAndReturnId(installmentId, loanId, memberId, amount, reference);
    }

    public int saveAndReturnId(int installmentId, int loanId, int memberId,
                               double amount, String reference) throws SQLException {
        return saveAndReturnId(
                installmentId,
                loanId,
                memberId,
                BigDecimal.valueOf(amount),
                reference
        );
    }

    public int saveAndReturnId(int installmentId, int loanId, int memberId,
                               BigDecimal amount, String reference) throws SQLException {
        return saveAndReturnId(installmentId, loanId, memberId, amount, reference,
                LocalDate.now());
    }

    public int saveAndReturnId(int installmentId, int loanId, int memberId,
                               BigDecimal amount, String reference, LocalDate paymentDate)
            throws SQLException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Payment reference is required");
        }
        if (paymentDate == null) {
            throw new IllegalArgumentException("Payment date is required");
        }

        String sql = """
                INSERT INTO payments(
                    installment_id,
                    loan_id,
                    member_id,
                    amount,
                    payment_reference,
                    payment_date,
                    status,
                    reversal_of_payment_id
                )
                VALUES(?,?,?,?,?,?,'COMPLETED',NULL)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, installmentId);
            ps.setInt(2, loanId);
            ps.setInt(3, memberId);
            ps.setBigDecimal(4, amount);
            ps.setString(5, reference);
            ps.setString(6, paymentDate.toString());
            ps.executeUpdate();
        }

        return lastInsertId();
    }

    public boolean referenceExists(String reference) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM payments WHERE payment_reference=?")) {
            ps.setString(1, reference);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public java.util.List<Payment> findRecent() throws SQLException {
        java.util.List<Payment> result = new java.util.ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT payment_id, installment_id, loan_id, member_id, amount,
                       payment_reference, status, reversal_of_payment_id
                FROM payments ORDER BY payment_id DESC LIMIT 50
                """); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapPayment(rs));
        }
        return result;
    }

    public Optional<Payment> findById(int id) throws SQLException {
        String sql = """
                SELECT payment_id,
                       installment_id,
                       loan_id,
                       member_id,
                       amount,
                       payment_reference,
                       status,
                       reversal_of_payment_id
                FROM payments
                WHERE payment_id=?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapPayment(rs));
            }
        }
    }

    public Optional<Payment> findReversalByOriginalPayment(int originalPaymentId)
            throws SQLException {
        String sql = """
                SELECT payment_id,
                       installment_id,
                       loan_id,
                       member_id,
                       amount,
                       payment_reference,
                       status,
                       reversal_of_payment_id
                FROM payments
                WHERE reversal_of_payment_id=?
                LIMIT 1
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, originalPaymentId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapPayment(rs));
            }
        }
    }

    public int saveReversal(Payment originalPayment) throws SQLException {
        String sql = """
                INSERT INTO payments(
                    installment_id,
                    loan_id,
                    member_id,
                    amount,
                    payment_reference,
                    payment_date,
                    status,
                    reversal_of_payment_id
                )
                VALUES(?,?,?,?,?,CURRENT_TIMESTAMP,'REVERSAL',?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, originalPayment.getInstallmentId());
            ps.setInt(2, originalPayment.getLoanId());
            ps.setInt(3, originalPayment.getMemberId());
            ps.setBigDecimal(4, originalPayment.getAmountDecimal().negate());
            ps.setString(5, "REVERSAL-" + originalPayment.getId());
            ps.setInt(6, originalPayment.getId());
            ps.executeUpdate();
        }

        return lastInsertId();
    }

    public void updateStatus(int id, String status) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                UPDATE payments
                SET status=?
                WHERE payment_id=?
                """)) {
            ps.setString(1, status);
            ps.setInt(2, id);

            if (ps.executeUpdate() != 1) {
                throw new SQLException("Payment status update affected an unexpected number of rows");
            }
        }
    }

    private int lastInsertId() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT last_insert_rowid()")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        throw new SQLException("Could not determine saved payment ID");
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        Payment payment = new Payment();
        payment.setId(rs.getInt("payment_id"));
        payment.setInstallmentId(rs.getInt("installment_id"));
        payment.setLoanId(rs.getInt("loan_id"));
        payment.setMemberId(rs.getInt("member_id"));
        payment.setAmount(rs.getBigDecimal("amount"));
        payment.setReference(rs.getString("payment_reference"));
        payment.setStatus(rs.getString("status"));

        int reversalOfPaymentId = rs.getInt("reversal_of_payment_id");
        payment.setReversalOfPaymentId(
                rs.wasNull() ? null : reversalOfPaymentId
        );
        return payment;
    }
}
