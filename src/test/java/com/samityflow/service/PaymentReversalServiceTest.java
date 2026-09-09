package com.samityflow.service;

import com.samityflow.database.Database;
import com.samityflow.model.Payment;
import com.samityflow.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
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

    private Path databasePath;
    private Database database;
    private PaymentReversalService service;

    @BeforeEach
    void setUp() {
        databasePath = tempDirectory.resolve("reversal-test.db");
        database = new Database("jdbc:sqlite:" + databasePath);
        database.initialize();
        database.seed();
        service = new PaymentReversalService(database);
    }

    @Test
    void successfulPartialPaymentReversalPreservesOriginalAndCreatesLinkedHistoryRecord()
            throws Exception {
        Fixture fixture = fixture(
                400, 1_000, 1_000, 5_000,
                LocalDate.now().plusDays(7), "PAID"
        );

        service.reversePayment(fixture.paymentId());

        try (Connection connection = database.connect()) {
            PaymentRepository payments = new PaymentRepository(connection);
            Payment original = payments.findById(fixture.paymentId()).orElseThrow();
            Payment reversal = payments.findReversalByOriginalPayment(fixture.paymentId()).orElseThrow();

            assertEquals(fixture.paymentId(), original.getId());
            assertEquals(400.0, original.getAmount(), 0.001);
            assertEquals("PAY-FIXTURE", original.getReference());
            assertEquals("REVERSED", original.getStatus());

            assertEquals("REVERSAL", reversal.getStatus());
            assertEquals(-400.0, reversal.getAmount(), 0.001);
            assertEquals(Integer.valueOf(fixture.paymentId()), reversal.getReversalOfPaymentId());
            assertEquals(fixture.installmentId(), reversal.getInstallmentId());
            assertEquals(fixture.loanId(), reversal.getLoanId());
            assertNotNull(reversal.getReference());

            assertEquals(2, count(connection, "SELECT COUNT(*) FROM payments"));
            assertEquals(1, count(connection,
                    "SELECT COUNT(*) FROM payments WHERE reversal_of_payment_id=" + fixture.paymentId()));
        }
    }

    @Test
    void successfulFullPaymentReversalReturnsFutureInstallmentToPending() throws Exception {
        Fixture fixture = fixture(
                1_000, 1_000, 1_000, 4_000,
                LocalDate.now().plusDays(7), "PAID"
        );

        service.reversePayment(fixture.paymentId());

        try (Connection connection = database.connect()) {
            assertMoney("0", scalarDecimal(connection,
                    "SELECT paid_amount FROM installments WHERE installment_id=?",
                    fixture.installmentId()));
            assertEquals("PENDING", scalarString(connection,
                    "SELECT status FROM installments WHERE installment_id=?",
                    fixture.installmentId()));
            assertMoney("5000", scalarDecimal(connection,
                    "SELECT outstanding_balance FROM loans WHERE loan_id=?",
                    fixture.loanId()));
        }
    }

    @Test
    void reversalSubtractsOnlyOriginalPaymentAndChangesPaidToPartiallyPaid() throws Exception {
        Fixture fixture = fixture(
                400, 1_000, 1_000, 5_000,
                LocalDate.now().plusDays(7), "PAID"
        );

        service.reversePayment(fixture.paymentId());

        try (Connection connection = database.connect()) {
            assertMoney("600", scalarDecimal(connection,
                    "SELECT paid_amount FROM installments WHERE installment_id=?",
                    fixture.installmentId()));
            assertEquals("PARTIALLY_PAID", scalarString(connection,
                    "SELECT status FROM installments WHERE installment_id=?",
                    fixture.installmentId()));
            assertMoney("5400", scalarDecimal(connection,
                    "SELECT outstanding_balance FROM loans WHERE loan_id=?",
                    fixture.loanId()));
        }
    }

    @Test
    void zeroPaidPastDueInstallmentBecomesOverdueEvenWhenItWasPreviouslyPaid() throws Exception {
        Fixture fixture = fixture(
                500, 1_000, 500, 5_000,
                LocalDate.now().minusDays(1), "PAID"
        );

        service.reversePayment(fixture.paymentId());

        try (Connection connection = database.connect()) {
            assertMoney("0", scalarDecimal(connection,
                    "SELECT paid_amount FROM installments WHERE installment_id=?",
                    fixture.installmentId()));
            assertEquals("OVERDUE", scalarString(connection,
                    "SELECT status FROM installments WHERE installment_id=?",
                    fixture.installmentId()));
        }
    }

    @Test
    void successfulReversalCreatesOneDetailedAuditEntry() throws Exception {
        Fixture fixture = fixture(
                400, 1_000, 1_000, 5_000,
                LocalDate.now().plusDays(7), "PAID"
        );

        service.reversePayment(fixture.paymentId());

        try (Connection connection = database.connect()) {
            assertEquals(1, count(connection,
                    "SELECT COUNT(*) FROM audit_logs WHERE action='PAYMENT_REVERSED'"));

            String details = scalarString(connection,
                    "SELECT details FROM audit_logs WHERE action='PAYMENT_REVERSED'");
            assertTrue(details.contains("originalPaymentId=" + fixture.paymentId()));
            assertTrue(details.contains("reversalPaymentId="));
            assertTrue(details.contains("loanId=" + fixture.loanId()));
            assertTrue(details.contains("installmentId=" + fixture.installmentId()));
            assertTrue(details.contains("amount=400"));
        }
    }

    @Test
    void secondReversalIsRejectedAndChangesNothing() throws Exception {
        Fixture fixture = fixture(
                400, 1_000, 1_000, 5_000,
                LocalDate.now().plusDays(7), "PAID"
        );

        service.reversePayment(fixture.paymentId());

        State afterFirst = readState(fixture);
        assertThrows(IllegalStateException.class, () -> service.reversePayment(fixture.paymentId()));
        State afterSecond = readState(fixture);

        assertEquals(afterFirst, afterSecond);
        assertEquals(1, afterSecond.reversalCount());
        assertEquals(1, afterSecond.auditCount());
        assertEquals("REVERSED", afterSecond.originalStatus());
        assertMoney("600", afterSecond.installmentPaid());
        assertMoney("5400", afterSecond.loanOutstanding());
    }

    @Test
    void negativeRestoredPaidAmountIsRejectedWithoutAnyPersistedChange() throws Exception {
        Fixture fixture = fixture(
                600, 1_000, 500, 5_000,
                LocalDate.now().plusDays(7), "PARTIALLY_PAID"
        );

        State before = readState(fixture);
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.reversePayment(fixture.paymentId())
        );
        State after = readState(fixture);

        assertTrue(exception.getMessage().contains("negative"));
        assertEquals(before, after);
        assertEquals(0, after.reversalCount());
        assertEquals(0, after.auditCount());
        assertEquals("COMPLETED", after.originalStatus());
    }

    @Test
    void lateAuditFailureRollsBackOriginalReversalInstallmentLoanAndAudit() throws Exception {
        Fixture fixture = fixture(
                400, 1_000, 1_000, 5_000,
                LocalDate.now().plusDays(7), "PAID"
        );
        State before = readState(fixture);

        try (Connection connection = database.connect(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TRIGGER fail_payment_reversal_audit
                    BEFORE INSERT ON audit_logs
                    WHEN NEW.action='PAYMENT_REVERSED'
                    BEGIN
                        SELECT RAISE(ABORT, 'forced audit failure');
                    END
                    """);
        }

        assertThrows(RuntimeException.class, () -> service.reversePayment(fixture.paymentId()));
        State after = readState(fixture);

        assertEquals(before, after);
        assertEquals("COMPLETED", after.originalStatus());
        assertEquals(0, after.reversalCount());
        assertEquals(0, after.auditCount());
    }

    @Test
    void reversalAndDuplicateProtectionSurviveDatabaseReload() throws Exception {
        Fixture fixture = fixture(
                400, 1_000, 1_000, 5_000,
                LocalDate.now().plusDays(7), "PAID"
        );

        service.reversePayment(fixture.paymentId());

        Database reloadedDatabase = new Database("jdbc:sqlite:" + databasePath);
        reloadedDatabase.initialize();
        PaymentReversalService reloadedService = new PaymentReversalService(reloadedDatabase);

        assertThrows(
                IllegalStateException.class,
                () -> reloadedService.reversePayment(fixture.paymentId())
        );

        try (Connection connection = reloadedDatabase.connect()) {
            assertEquals(1, count(connection,
                    "SELECT COUNT(*) FROM payments WHERE reversal_of_payment_id=" + fixture.paymentId()));
            assertEquals("REVERSED", scalarString(connection,
                    "SELECT status FROM payments WHERE payment_id=?", fixture.paymentId()));
            assertEquals(1, count(connection,
                    "SELECT COUNT(*) FROM audit_logs WHERE action='PAYMENT_REVERSED'"));
        }
    }

    @Test
    void collectionThenReversalRestoresExactPreCollectionFinancialState() throws Exception {
        BaseFixture base = fixtureWithoutPayment(
                1_000, 600, 5_000,
                LocalDate.now().plusDays(7), "PARTIALLY_PAID"
        );

        WeeklyCollectionService collectionService = new WeeklyCollectionService(database);
        CollectionResult result = collectionService.collectPayment(
                base.installmentId(),
                base.loanId(),
                1,
                new BigDecimal("400"),
                BigDecimal.ZERO,
                LocalDate.now()
        );

        assertTrue(result.isSuccess(), result.getMessage());

        int paymentId;
        try (Connection connection = database.connect()) {
            paymentId = scalarInt(connection,
                    "SELECT payment_id FROM payments WHERE installment_id=? AND status='COMPLETED'",
                    base.installmentId());
            assertMoney("1000", scalarDecimal(connection,
                    "SELECT paid_amount FROM installments WHERE installment_id=?",
                    base.installmentId()));
            assertEquals("PAID", scalarString(connection,
                    "SELECT status FROM installments WHERE installment_id=?",
                    base.installmentId()));
            assertMoney("4600", scalarDecimal(connection,
                    "SELECT outstanding_balance FROM loans WHERE loan_id=?",
                    base.loanId()));
        }

        service.reversePayment(paymentId);

        try (Connection connection = database.connect()) {
            assertMoney("600", scalarDecimal(connection,
                    "SELECT paid_amount FROM installments WHERE installment_id=?",
                    base.installmentId()));
            assertEquals("PARTIALLY_PAID", scalarString(connection,
                    "SELECT status FROM installments WHERE installment_id=?",
                    base.installmentId()));
            assertMoney("5000", scalarDecimal(connection,
                    "SELECT outstanding_balance FROM loans WHERE loan_id=?",
                    base.loanId()));
            assertEquals("REVERSED", scalarString(connection,
                    "SELECT status FROM payments WHERE payment_id=?", paymentId));
            assertEquals(1, count(connection,
                    "SELECT COUNT(*) FROM payments WHERE reversal_of_payment_id=" + paymentId));
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
        BaseFixture base = fixtureWithoutPayment(
                installmentTotal,
                installmentPaid,
                loanOutstanding,
                dueDate,
                installmentStatus
        );

        try (Connection connection = database.connect()) {
            PaymentRepository payments = new PaymentRepository(connection);
            int paymentId = payments.saveAndReturnId(
                    base.installmentId(),
                    base.loanId(),
                    1,
                    BigDecimal.valueOf(paymentAmount),
                    "PAY-FIXTURE"
            );
            return new Fixture(paymentId, base.installmentId(), base.loanId());
        }
    }

    private BaseFixture fixtureWithoutPayment(
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
                    ps.setBigDecimal(2, BigDecimal.valueOf(loanOutstanding));
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
                    ps.setBigDecimal(3, BigDecimal.valueOf(installmentTotal));
                    ps.setBigDecimal(4, BigDecimal.valueOf(installmentPaid));
                    ps.setString(5, installmentStatus);
                    ps.executeUpdate();
                    installmentId = lastInsertId(connection);
                }

                connection.commit();
                return new BaseFixture(installmentId, loanId);
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof SQLException sqlException) {
                    throw sqlException;
                }
                throw new SQLException("Could not create payment reversal fixture", exception);
            }
        }
    }

    private State readState(Fixture fixture) throws SQLException {
        try (Connection connection = database.connect()) {
            return new State(
                    scalarString(connection,
                            "SELECT status FROM payments WHERE payment_id=?", fixture.paymentId()),
                    count(connection,
                            "SELECT COUNT(*) FROM payments WHERE reversal_of_payment_id=" + fixture.paymentId()),
                    scalarDecimal(connection,
                            "SELECT paid_amount FROM installments WHERE installment_id=?",
                            fixture.installmentId()),
                    scalarString(connection,
                            "SELECT status FROM installments WHERE installment_id=?",
                            fixture.installmentId()),
                    scalarDecimal(connection,
                            "SELECT outstanding_balance FROM loans WHERE loan_id=?",
                            fixture.loanId()),
                    count(connection,
                            "SELECT COUNT(*) FROM audit_logs WHERE action='PAYMENT_REVERSED'")
            );
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

    private int scalarInt(Connection connection, String sql, Object... values)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, values);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private BigDecimal scalarDecimal(Connection connection, String sql, Object... values)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, values);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
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

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private record Fixture(int paymentId, int installmentId, int loanId) {
    }

    private record BaseFixture(int installmentId, int loanId) {
    }

    private record State(
            String originalStatus,
            int reversalCount,
            BigDecimal installmentPaid,
            String installmentStatus,
            BigDecimal loanOutstanding,
            int auditCount
    ) {
    }
}
