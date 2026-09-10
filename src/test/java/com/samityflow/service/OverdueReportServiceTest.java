package com.samityflow.service;

import com.samityflow.AppContext;
import com.samityflow.database.Database;
import com.samityflow.model.LoanApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OverdueReportServiceTest {
    @TempDir Path tempDirectory;
    private Database database;
    private AppContext context;

    @BeforeEach
    void setUp() {
        database = new Database("jdbc:sqlite:" + tempDirectory.resolve("reports.db"));
        context = new AppContext(database);
    }

    @Test
    void overdueScanAppliesPenaltyRestrictsMemberAndNotifiesObserver() throws Exception {
        LoanApplication application = approvedEmergencyApplication();
        context.disbursementService.disburse(
                application.getId(), LocalDate.now().minusWeeks(5), "WEEKLY");
        List<String> alerts = new ArrayList<>();
        context.overdueService.addObserver(alerts::add);

        double before = scalar(database, "SELECT outstanding_balance FROM loans LIMIT 1");

        int updated = context.overdueService.scan(LocalDate.now());

        double penalties = scalar(database, "SELECT SUM(penalty_amount) FROM installments");
        double after = scalar(database, "SELECT outstanding_balance FROM loans LIMIT 1");

        assertTrue(updated > 0);
        assertFalse(alerts.isEmpty());
        assertEquals(before + penalties, after, 0.001);
        try (Connection connection = database.connect(); Statement statement = connection.createStatement()) {
            assertTrue(number(statement,
                    "SELECT penalty_amount FROM installments WHERE status='OVERDUE' ORDER BY installment_id LIMIT 1") > 0);
            assertEquals(0, number(statement, "SELECT eligible FROM members WHERE id=1"));
            assertEquals("DEFAULTED", text(statement, "SELECT status FROM loans LIMIT 1"));
        }


        context.overdueService.scan(LocalDate.now());
        assertEquals(after, scalar(database,
                "SELECT outstanding_balance FROM loans LIMIT 1"), 0.001,
                "Scanning the same overdue items twice must not add the penalty twice");
    }

    @Test
    void reportsShowLoansSavingsAndMemberBalances() {
        LoanApplication application = approvedEmergencyApplication();
        context.disbursementService.disburse(application.getId(), LocalDate.now().plusWeeks(1), "WEEKLY");
        context.savingsService.deposit(1, 750, "SAV-REPORT-1");

        var portfolio = context.reportService.portfolioSummary();
        var collection = context.reportService.collectionSummary();
        var members = context.reportService.memberFinancialReport();

        assertEquals(1, portfolio.activeLoans());
        assertEquals(750, portfolio.savingsBalance().doubleValue(), 0.001);
        assertTrue(portfolio.outstanding().doubleValue() > 5_000);
        assertTrue(collection.expected().doubleValue() > 5_000);
        assertEquals(3, members.size());
        assertEquals(750, members.stream().filter(row -> row.memberId() == 1)
                .findFirst().orElseThrow().savings().doubleValue(), 0.001);
    }

    private LoanApplication approvedEmergencyApplication() {
        LoanApplication application = context.loanService.createAndSubmit(1, 3, 5_000, "Medical cost");
        context.loanService.addGuarantee(application.getId(), 2);
        context.loanService.officerApprove(application.getId());
        context.loanService.managerApprove(application.getId(), "Approved");
        return context.applications.findById(application.getId()).orElseThrow();
    }

    private double number(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) { return result.getDouble(1); }
    }
    private String text(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) { return result.getString(1); }
    }
    private double scalar(Database source, String sql) throws Exception {
        try (Connection connection = source.connect(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            return result.getDouble(1);
        }
    }
}
