package com.samityflow.service;

import com.samityflow.database.Database;
import com.samityflow.model.Payment;
import com.samityflow.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentReversalServiceTest {

    @TempDir
    Path tempDirectory;

    private Database database;
    private PaymentReversalService service;

    @BeforeEach
    void setUp() {
        database = new Database("jdbc:sqlite:" + tempDirectory.resolve("reversal-test.db"));
        database.initialize();
        database.seed();
        service = new PaymentReversalService(database);
    }

    @Test
    void successfulReversalPreservesOriginalAndCreatesLinkedReversal() throws Exception {
        Fixture fixture = fixture(400, 1_000, 1_000, 5_000, LocalDate.now().plusDays(7), "PAID");

        service.reversePayment(fixture.paymentId());

        try (Connection connection = database.connect()) {
            PaymentRepository payments = new PaymentRepository(connection);
            Payment original = payments.findById(fixture.paymentId()).orElseThrow();
            Payment reversal = payments.findReversalByOriginalPayment(fixture.paymentId()).orElseThrow();

            assertEquals(fixture.paymentId(), original.getId());
            assertEquals("REVERSED", original.getStatus());
            assertEquals("REVERSAL", reversal.getStatus());
            assertEquals(-400.0, reversal.getAmount(), 0.001);
            assertEquals(Integer.valueOf(fixture.paymentId()), reversal.getReversalOfPaymentId());
            assertNotNull(reversal.getReference());
            assertEquals(2, count(connection, "SELECT COUNT(*) FROM payments"));
        }
    }

    @Test
    void reversalRestoresOnlyTheOriginalPaymentAmountFromInstallment() throws Exception {
        Fixture fixture = fixture(400, 1_000, 1_000, 5_000, LocalDate.now().plusDays(7), "PAID");

        service.reversePayment(fixture.paymentId());

        try (Connection connection = database.connect()) {
            assertEquals(600.0, scalarDouble(connection,
                    "SELECT paid_amount FROM installments WHERE installment_id=?",
                    fixture.installmentId()), 0.001);
            assertEquals("PARTIALLY_PAID", scalarString(connection,
                    "SELECT status FROM installments WHERE installment_id=?",
                    fixture.installmentId()));
        }
    }

    @Test
    void fullRestorationReturnsFutureInstallmentToPending() throws Exception {
        Fixture fixture = fixture(500, 1_000, 500, 5_000, LocalDate.now().plusDays(7), "PARTIALLY_PAID");

        service.reversePayment(fixture.paymentId());

        try (Connection connection = database.connect()) {
            assertEquals(0.0, scalarDouble(connection,
                    "SELECT paid_amount FROM installments WHERE installment_id=?",
                    fixture.installmentId()), 0.001);
            assertEquals("PENDING", scalarString(connection,
                    "SELECT status FROM installments WHERE installment_id=?",
                    fixture.installmentId()));
        }
    }

    @Test
    void fullRestorationPreservesOverdueBehaviorForPastDueInstallment() throws Exception {
        Fixture fixture = fixture(500, 1_000, 500, 5_000, LocalDate.now().minusDays(1), "OVERDUE");

        service.reversePayment(fixture.paymentId());

        try (Connection connection = database.connect()) {
            assertEquals(0.0, scalarDouble(connection,
                    "SELECT paid_amount FROM installments WHERE installment_id=?",
                    fixture.installmentId()), 0.001);
            assertEquals("OVERDUE", scalarString(connection,
                    "SELECT status FROM installments WHERE installment_id=?",
                    fixture.installmentId()));
        }
    }

    @Test
    void reversalRestoresLoanOutstandingByExactPaymentAmount() throws Exception {
        Fixture fixture = fixture(500, 1_000, 500, 5_000, LocalDate.now().plusDays(7), "PARTIALLY_PAID");

        service.reversePayment(fixture.paymentId());

        try (Connection connection = database.connect()) {
            assertEquals(5_500.0, scalarDouble(connection,
                    "SELECT outstanding_balance FROM loans WHERE loan_id=?",
                    fixture.loanId()), 0.001);
        }
    }

    @Test
    void successfulReversalCreatesExactlyOneAuditEntry() throws Exception {
        Fixture fixture = fixture(400, 1_000, 1_000, 5_000, LocalDate.now().plusDays(7), "PAID");

        service.reversePayment(fixture.paymentId());

        try (Connection connection = database.connect()) {
            assertEquals(1, count(connection,
                    "SELECT COUNT(*) FROM audit_logs WHERE action='PAYMENT_REVERSAL'"));
            String details = scalarString(connection,
                    "SELECT details FROM audit_logs WHERE action='PAYMENT_REVERSAL'");
            assertTrue(details.contains("Original payment " + fixture.paymentId()));
            assertTrue(details.contains("amount=400.0"));
        }
    }

    @Test
    void secondReversalIsRejectedWithoutSecondBalanceAdjustment() throws Exception {
        Fixture fixture = fixture(400, 1_000, 1_000, 5_000, LocalDate.now().plusDays(7), "PAID");

        service.reversePayment(fixture.paymentId());
        assertThrows(IllegalStateException.class, () -> service.reversePayment(fixture.paymentId()));

        try (Connection connection = database.connect()) {
            assertEquals(1, count(connection,
                    "SELECT COUNT(*) FROM payments WHERE reversal_of_payment_id=" + fixture.paymentId()));
            assertEquals(600.0, scalarDouble(connection,
                    "SELECT paid_amount FROM installments WHERE installment_id=?",
                    fixture.installmentId()), 0.001);
            assertEquals(5_400.0, scalarDouble(connection,
                    "SELECT outstanding_balance FROM loans WHERE loan_id=?",
                    fixture.loanId()), 0.001);
            assertEquals(1, count(connection,
                    "SELECT COUNT(*) FROM audit_logs WHERE action='PAYMENT_REVERSAL'"));
        }
    }

    @Test
    void lateAuditFailureRollsBackEveryReversalChange() throws Exception {
        Fixture fixture = fixture(400, 1_000, 1_000, 5_000, LocalDate.now().plusDays(7), "PAID");

        try (Connection connection = database.connect(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TRIGGER fail_payment_reversal_audit
                    BEFORE INSERT ON audit_logs
                    WHEN NEW.action='PAYMENT_REVERSAL'
                    BEGIN
                        SELECT RAISE(ABORT, 'forced audit failure');
                    END
                    """);
        }

        assertThrows(RuntimeException.class, () -> service.reversePayment(fixture.paymentId()));

        try (Connection connection = database.connect()) {
            assertEquals("COMPLETED", scalarString(connection,
                    "SELECT status FROM payments WHERE payment_id=?",
                    fixture.paymentId()));
            assertEquals(0, count(connection,
                    "SELECT COUNT(*) FROM payments WHERE reversal_of_payment_id=" + fixture.paymentId()));
            assertEquals(1_000.0, scalarDouble(connection,
                    "SELECT paid_amount FROM installments WHERE installment_id=?",
                    fixture.installmentId()), 0.001);
            assertEquals("PAID", scalarString(connection,
                    "SELECT status FROM installments WHERE installment_id=?",
                    fixture.installmentId()));
            assertEquals(5_000.0, scalarDouble(connection,
                    "SELECT outstanding_balance FROM loans WHERE loan_id=?",
                    fixture.loanId()), 0.001);
            assertEquals(0, count(connection,
                    "SELECT COUNT(*) FROM audit_logs WHERE action='PAYMENT_REVERSAL'"));
        }
    }

    private Fixture fixture(
            double paymentAmount,
            double installmentTotal,
            double installmentPaid,
            double loanOutstanding,
            LocalDate dueDate,
            String installmentStatus
    ) throws SQLException {
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                int applicationId = insertAndGetId(connection, """
                        INSERT INTO loan_applications(
                            member_id, product_id, amount, purpose, status,
                            officer_approved, manager_comment
                        ) VALUES(1,1,10000,'Payment reversal fixture','APPROVED',1,'approved')
                        """);

                int loanId;
                try (PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO loans(
                            application_id, member_id, principal,
                            outstanding_balance, status
                        ) VALUES(?,1,10000,?,'ACTIVE')
                        """)) {
                    ps.setInt(1, applicationId);
                    ps.setDouble(2, loanOutstanding);
                    ps.executeUpdate();
                    loanId = lastInsertId(connection);
                }

                int installmentId;
                try (PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO installments(
                            loan_id, installment_number, due_date,
                            principal_amount, interest_amount, total_amount,
                            penalty_amount, paid_amount, status
                        ) VALUES(?,1,?,800,200,?,0,?,?)
                        """)) {
                    ps.setInt(1, loanId);
                    ps.setString(2, dueDate.toString());
                    ps.setDouble(3, installmentTotal);
                    ps.setDouble(4, installmentPaid);
                    ps.setString(5, installmentStatus);
                    ps.executeUpdate();
                    installmentId = lastInsertId(connection);
                }

                PaymentRepository payments = new PaymentRepository(connection);
                int paymentId = payments.saveAndReturnId(
                        installmentId,
                        loanId,
                        1,
                        paymentAmount,
                        "PAY-FIXTURE"
                );

                connection.commit();
                return new Fixture(paymentId, installmentId, loanId);
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof SQLException sqlException) {
                    throw sqlException;
                }
                throw new SQLException("Could not create payment reversal fixture", exception);
            }
        }
    }

    private int insertAndGetId(Connection connection, String sql) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
            return lastInsertId(connection);
        }
    }

    private int lastInsertId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT last_insert_rowid()")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        throw new SQLException("Expected a generated ID");
    }

    private int count(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private double scalarDouble(Connection connection, String sql, Object... values)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, values);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        }
    }

    private String scalarString(Connection connection, String sql, Object... values)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, values);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private void bind(PreparedStatement ps, Object... values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            ps.setObject(i + 1, values[i]);
        }
    }

    private record Fixture(int paymentId, int installmentId, int loanId) {
    }
}
