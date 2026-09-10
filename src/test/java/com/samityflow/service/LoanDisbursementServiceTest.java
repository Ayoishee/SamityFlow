package com.samityflow.service;

import com.samityflow.AppContext;
import com.samityflow.database.Database;
import com.samityflow.model.ApplicationStatus;
import com.samityflow.model.Loan;
import com.samityflow.model.LoanApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LoanDisbursementServiceTest {
    @TempDir Path tempDirectory;
    private Database database;
    private AppContext context;

    @BeforeEach
    void setUp() {
        database = new Database("jdbc:sqlite:" + tempDirectory.resolve("disbursement.db"));
        context = new AppContext(database);
    }

    @Test
    void approvedApplicationCreatesActiveLoanAndFixedSchedule() throws Exception {
        LoanApplication application = approvedEmergencyApplication();

        Loan loan = context.disbursementService.disburse(
                application.getId(), LocalDate.of(2026, 1, 4), "WEEKLY");

        assertEquals("ACTIVE", loan.getStatus());
        assertTrue(loan.getOutstanding() > application.getAmount());
        try (Connection connection = database.connect(); Statement statement = connection.createStatement()) {
            assertEquals(12, count(statement,
                    "SELECT COUNT(*) FROM installments WHERE loan_id=" + loan.getId()));
            assertEquals(1, count(statement,
                    "SELECT COUNT(*) FROM audit_logs WHERE action='LOAN_DISBURSED'"));
        }
    }

    @Test
    void applicationCannotBeDisbursedTwice() {
        LoanApplication application = approvedEmergencyApplication();
        context.disbursementService.disburse(application.getId(), LocalDate.now(), "WEEKLY");

        assertThrows(IllegalStateException.class, () ->
                context.disbursementService.disburse(application.getId(), LocalDate.now(), "WEEKLY"));
    }

    @Test
    void pendingApplicationCannotBeDisbursed() {
        LoanApplication application = context.loanService.createAndSubmit(1, 3, 5_000, "Medical cost");
        assertEquals(ApplicationStatus.GUARANTEE_PENDING, application.getStatus());

        assertThrows(IllegalStateException.class, () ->
                context.disbursementService.disburse(application.getId(), LocalDate.now(), "WEEKLY"));
    }

    private LoanApplication approvedEmergencyApplication() {
        LoanApplication application = context.loanService.createAndSubmit(1, 3, 5_000, "Medical cost");
        context.loanService.addGuarantee(application.getId(), 2);
        context.loanService.officerApprove(application.getId());
        context.loanService.managerApprove(application.getId(), "Approved");
        return context.applications.findById(application.getId()).orElseThrow();
    }

    private int count(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) { return result.getInt(1); }
    }
}
