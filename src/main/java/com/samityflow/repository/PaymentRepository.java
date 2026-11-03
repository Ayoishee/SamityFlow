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

/** Persists and queries Payment records using JDBC. */
public class PaymentRepository {

    private final Connection connection;

    public PaymentRepository(Connection connection) {
        this.connection = connection;
    }

    public void save(int installmentId, int loanId, int memberId, double amount, String reference)
            throws SQLException {
        save(installmentId, loanId, memberId, BigDecimal.valueOf(amount), reference);
    }

    public void save(
            int installmentId, int loanId, int memberId, BigDecimal amount, String reference)
            throws SQLException {
        saveAndReturnId(installmentId, loanId, memberId, amount, reference);
    }

    public int saveAndReturnId(
            int installmentId, int loanId, int memberId, double amount, String reference)
            throws SQLException {
        return saveAndReturnId(
                installmentId, loanId, memberId, BigDecimal.valueOf(amount), reference);
    }

    public int saveAndReturnId(
            int installmentId, int loanId, int memberId, BigDecimal amount, String reference)
            throws SQLException {
        return saveAndReturnId(installmentId, loanId, memberId, amount, reference, LocalDate.now());
    }

    public int saveAndReturnId(
            int installmentId,
            int loanId,
            int memberId,
            BigDecimal amount,
            String reference,
            LocalDate paymentDate)
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

        String sql =
                """
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

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, installmentId);
            preparedStatement.setInt(2, loanId);
            preparedStatement.setInt(3, memberId);
            preparedStatement.setBigDecimal(4, amount);
            preparedStatement.setString(5, reference);
            preparedStatement.setString(6, paymentDate.toString());
            preparedStatement.executeUpdate();
        }

        int paymentId = lastInsertId();
        recordAllocation(paymentId, installmentId, amount);
        return paymentId;
    }

    private void recordAllocation(int paymentId, int installmentId, BigDecimal amount)
            throws SQLException {
        try (PreparedStatement query =
                connection.prepareStatement(
                        "SELECT interest_amount,total_amount,paid_amount FROM installments WHERE"
                                + " installment_id=?")) {
            query.setInt(1, installmentId);
            try (ResultSet result = query.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Installment not found");
                }
                BigDecimal interest =
                        com.samityflow.model.Money.round(result.getBigDecimal("interest_amount"));
                BigDecimal total =
                        com.samityflow.model.Money.round(result.getBigDecimal("total_amount"));
                BigDecimal before =
                        com.samityflow.model.Money.round(result.getBigDecimal("paid_amount"));
                BigDecimal after = before.add(amount);
                BigDecimal interestPaid = after.min(interest).subtract(before.min(interest));
                BigDecimal basePaid = after.min(total).subtract(before.min(total));
                try (PreparedStatement update =
                        connection.prepareStatement(
                                "UPDATE payments SET"
                                        + " interest_paid=?,principal_paid=?,penalty_paid=? WHERE"
                                        + " payment_id=?")) {
                    update.setBigDecimal(1, interestPaid);
                    update.setBigDecimal(2, basePaid.subtract(interestPaid));
                    update.setBigDecimal(3, amount.subtract(basePaid));
                    update.setInt(4, paymentId);
                    update.executeUpdate();
                }
            }
        }
    }

    public boolean referenceExists(String reference) throws SQLException {
        try (PreparedStatement preparedStatement =
                connection.prepareStatement(
                        "SELECT COUNT(*) FROM payments WHERE payment_reference=?")) {
            preparedStatement.setString(1, reference);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public java.util.List<Payment> findRecent() throws SQLException {
        java.util.List<Payment> result = new java.util.ArrayList<>();
        try (PreparedStatement preparedStatement =
                        connection.prepareStatement(
                                """
                                SELECT payment_id, installment_id, loan_id, member_id, amount,
                                       payment_reference, status, reversal_of_payment_id
                                FROM payments ORDER BY payment_id DESC LIMIT 50
                                """);
                ResultSet rs = preparedStatement.executeQuery()) {
            while (rs.next()) {
                result.add(mapPayment(rs));
            }
        }
        return result;
    }

    public Optional<Payment> findById(int id) throws SQLException {
        String sql =
                """
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

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapPayment(rs));
            }
        }
    }

    public Optional<Payment> findReversalByOriginalPayment(int originalPaymentId)
            throws SQLException {
        String sql =
                """
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

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, originalPaymentId);

            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapPayment(rs));
            }
        }
    }

    public int saveReversal(Payment originalPayment) throws SQLException {
        String sql =
                """
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

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, originalPayment.getInstallmentId());
            preparedStatement.setInt(2, originalPayment.getLoanId());
            preparedStatement.setInt(3, originalPayment.getMemberId());
            preparedStatement.setBigDecimal(4, originalPayment.getAmountDecimal().negate());
            preparedStatement.setString(5, "REVERSAL-" + originalPayment.getId());
            preparedStatement.setInt(6, originalPayment.getId());
            preparedStatement.executeUpdate();
        }

        int reversalId = lastInsertId();
        try (PreparedStatement update =
                connection.prepareStatement(
                        "UPDATE payments SET (principal_paid,interest_paid,penalty_paid)=(SELECT"
                                + " -principal_paid,-interest_paid,-penalty_paid FROM payments WHERE"
                                + " payment_id=?) WHERE payment_id=?")) {
            update.setInt(1, originalPayment.getId());
            update.setInt(2, reversalId);
            update.executeUpdate();
        }
        return reversalId;
    }

    public void updateStatus(int id, String status) throws SQLException {
        try (PreparedStatement preparedStatement =
                connection.prepareStatement(
                        """
                        UPDATE payments
                        SET status=?
                        WHERE payment_id=?
                        """)) {
            preparedStatement.setString(1, status);
            preparedStatement.setInt(2, id);

            if (preparedStatement.executeUpdate() != 1) {
                throw new SQLException(
                        "Payment status update affected an unexpected number of rows");
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
        payment.setAmount(com.samityflow.model.Money.round(rs.getBigDecimal("amount")));
        payment.setReference(rs.getString("payment_reference"));
        payment.setStatus(rs.getString("status"));

        int reversalOfPaymentId = rs.getInt("reversal_of_payment_id");
        payment.setReversalOfPaymentId(rs.wasNull() ? null : reversalOfPaymentId);
        return payment;
    }
}
