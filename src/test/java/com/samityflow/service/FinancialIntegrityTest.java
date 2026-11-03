package com.samityflow.service;

import static org.junit.jupiter.api.Assertions.*;

import com.samityflow.AppContext;
import com.samityflow.database.Database;
import com.samityflow.model.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/** Integration regressions exercise independent SQLite connections and persisted state. */
class FinancialIntegrityTest {
    @TempDir Path directory;
    Database database;
    AppContext context;

    @BeforeEach
    void prepare() {
        database = new Database("jdbc:sqlite:" + directory.resolve("finance.db"));
        context = new AppContext(database);
    }

    int approved() {
        int id = context.loanService.createAndSubmit(1, 3, 1200, "Treatment").getId();
        context.loanService.addGuarantee(id, 2);
        context.loanService.officerApprove(id);
        context.loanService.managerApprove(id, "Approved");
        return id;
    }

    int disburse() {
        context.disbursementService.disburse(approved(), LocalDate.now().minusWeeks(3), "WEEKLY");
        return (int) scalar("SELECT MIN(installment_id) FROM installments");
    }

    double scalar(String sql) {
        try (Connection connection = database.connect();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getDouble(1);
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    int collect(int installment, String reference, String amount, String savings) {
        CollectionResult result =
                context.collectionService.collectPayment(
                        installment,
                        1,
                        1,
                        new BigDecimal(amount),
                        new BigDecimal(savings),
                        LocalDate.now(),
                        reference);
        assertTrue(result.isSuccess(), result.getMessage());
        return (int) scalar("SELECT MAX(payment_id) FROM payments");
    }

    @Test
    void sameMemberMatchingReferenceIsNotAProvenLink() {
        int installment = disburse();
        context.savingsService.deposit(1, 30, "MATCH");
        context.reversalService.reversePayment(collect(installment, "MATCH", "10", "0"));
        assertEquals(30, context.savingsService.balance(1));
    }

    @Test
    void otherMemberMatchingReferenceCannotBeReversed() {
        int installment = disburse();
        context.savingsService.deposit(2, 30, "MATCH");
        context.reversalService.reversePayment(collect(installment, "MATCH", "10", "0"));
        assertEquals(30, context.savingsService.balance(2));
        assertEquals(0, context.savingsService.balance(1));
    }

    @Test
    void genuinelyLinkedSavingsReversedExactlyOnce() {
        int payment = collect(disburse(), "COLLECT", "10", "25");
        context.savingsService.deposit(1, 40, "UNRELATED");
        context.reversalService.reversePayment(payment);
        assertEquals(40, context.savingsService.balance(1));
        assertEquals(
                1,
                scalar(
                        "SELECT COUNT(*) FROM savings_transactions WHERE reversal_of_transaction_id"
                                + " IS NOT NULL"));
        assertThrows(
                IllegalStateException.class, () -> context.reversalService.reversePayment(payment));
        assertEquals(40, context.savingsService.balance(1));
    }

    @Test
    void spentLinkedSavingsRollsBackEntireReversal() {
        int payment = collect(disburse(), "COLLECT", "10", "25");
        context.savingsService.withdraw(1, 25, "SPENT");
        double outstanding = scalar("SELECT outstanding_balance FROM loans");
        assertThrows(
                IllegalStateException.class, () -> context.reversalService.reversePayment(payment));
        assertEquals(0, scalar("SELECT COUNT(*) FROM payments WHERE status='REVERSAL'"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM audit_logs WHERE action='PAYMENT_REVERSED'"));
        assertEquals(outstanding, scalar("SELECT outstanding_balance FROM loans"));
    }

    @Test
    void partialPenaltyPaymentAndRepeatedScansKeepChargesNonnegative() {
        int installment = disburse();
        LocalDate date = LocalDate.now();
        context.overdueService.scan(date);
        double penalty =
                scalar(
                        "SELECT penalty_amount FROM installments WHERE installment_id="
                                + installment);
        int payment = collect(installment, "PARTIAL", "106", "0");
        assertEquals(
                100, scalar("SELECT principal_paid FROM payments WHERE payment_id=" + payment));
        assertEquals(5, scalar("SELECT interest_paid FROM payments WHERE payment_id=" + payment));
        assertEquals(1, scalar("SELECT penalty_paid FROM payments WHERE payment_id=" + payment));
        double balance = scalar("SELECT outstanding_balance FROM loans");
        context.overdueService.scan(date);
        assertEquals(balance, scalar("SELECT outstanding_balance FROM loans"));
        assertEquals(
                penalty,
                scalar(
                        "SELECT penalty_amount FROM installments WHERE installment_id="
                                + installment));
        context.overdueService.scan(date.plusWeeks(1));
        assertEquals(
                penalty,
                scalar(
                        "SELECT penalty_amount FROM installments WHERE installment_id="
                                + installment));
    }

    @Test
    void concurrentWithdrawalsCannotOverdraw() throws Exception {
        context.savingsService.deposit(1, 100, "INITIAL");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> one = executor.submit(() -> withdrawAfter(start, "FIRST"));
            Future<Boolean> two = executor.submit(() -> withdrawAfter(start, "SECOND"));
            start.countDown();
            assertNotEquals(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS));
            assertEquals(20, context.savingsService.balance(1));
            assertEquals(
                    1,
                    scalar(
                            "SELECT COUNT(*) FROM savings_transactions WHERE"
                                    + " transaction_type='WITHDRAWAL'"));
        } finally {
            executor.shutdownNow();
        }
    }

    boolean withdrawAfter(CountDownLatch start, String reference) throws Exception {
        start.await();
        try {
            new SavingsService(database).withdraw(1, 80, reference);
            return true;
        } catch (IllegalArgumentException expected) {
            return false;
        }
    }

    void editProduct() {
        context.products.update(
                new LoanProduct(3, "Changed", "EMERGENCY", 90, "NORMAL", 90, 2, 0, 900));
    }

    @Test
    void productEditsAfterApprovalAndDisbursementPreserveTerms() {
        int application = approved();
        editProduct();
        Loan loan =
                context.disbursementService.disburse(
                        application, LocalDate.now().minusWeeks(3), "WEEKLY");
        assertEquals(1260, loan.getOutstanding());
        assertEquals(12, scalar("SELECT COUNT(*) FROM installments"));
        editProduct();
        context.overdueService.scan(LocalDate.now());
        assertEquals(
                3.15,
                scalar("SELECT penalty_amount FROM installments ORDER BY installment_id LIMIT 1"),
                0.0001);
        assertEquals(50, context.products.termsForApplication(application).weeklySavings());
    }

    @Test
    void zeroGuaranteesGoesDirectlyToOfficerReview() {
        context.products.update(
                new LoanProduct(3, "No guarantee", "EMERGENCY", 5, "GRACE", 1, 12, 0, 50));
        LoanApplication application = context.loanService.createAndSubmit(1, 3, 1200, "Treatment");
        assertEquals(ApplicationStatus.OFFICER_REVIEW, application.getStatus());
        editProduct();
        context.loanService.officerApprove(application.getId());
        context.loanService.managerApprove(application.getId(), "Approved");
        assertEquals(
                ApplicationStatus.APPROVED,
                context.applications.findById(application.getId()).orElseThrow().getStatus());
    }

    @Test
    void callerSuccessDoesNotCommitUnrelatedWork() throws Exception {
        int application = approved();
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO users(name,role) VALUES('Unrelated','MANAGER')");
            }
            new LoanDisbursementService(connection)
                    .disburse(application, LocalDate.now(), "WEEKLY");
            assertFalse(connection.getAutoCommit());
            assertEquals(0, scalar("SELECT COUNT(*) FROM loans"));
            connection.rollback();
        }
        assertEquals(0, scalar("SELECT COUNT(*) FROM loans"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM users WHERE name='Unrelated'"));
    }

    @Test
    void callerFailureRollsBackOnlyServiceSavepoint() throws Exception {
        int application = approved();
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO users(name,role) VALUES('Unrelated','MANAGER')");
            }
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            new LoanDisbursementService(connection)
                                    .disburse(application, LocalDate.now(), "INVALID"));
            assertFalse(connection.getAutoCommit());
            connection.commit();
        }
        assertEquals(0, scalar("SELECT COUNT(*) FROM loans"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM users WHERE name='Unrelated'"));
    }

    @Test
    void suppliedAutoCommitConnectionRestoresSettingOnSuccessAndFailure() throws Exception {
        int application = approved();
        try (Connection connection = database.connect()) {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            new LoanDisbursementService(connection)
                                    .disburse(application, LocalDate.now(), "INVALID"));
            assertTrue(connection.getAutoCommit());
            new LoanDisbursementService(connection)
                    .disburse(application, LocalDate.now(), "WEEKLY");
            assertTrue(connection.getAutoCommit());
            assertFalse(connection.isClosed());
        }
        assertEquals(1, scalar("SELECT COUNT(*) FROM loans"));
    }
}
