package com.samityflow;

import com.samityflow.database.Database;
import com.samityflow.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

class LoanApplicationServiceTest {
    @TempDir Path tempDirectory;
    private AppContext context;

    @BeforeEach
    void setup() {
        Database database = new Database("jdbc:sqlite:" + tempDirectory.resolve("test.db"));
        context = new AppContext(database);
    }

    @Test
    void memberCannotGuaranteeOwnApplication() {
        LoanApplication application = context.loanService.createAndSubmit(1, 3, 5_000, "Medical cost");
        assertThrows(IllegalArgumentException.class, () -> context.loanService.addGuarantee(application.getId(), 1));
    }

    @Test
    void duplicateGuaranteeIsPrevented() {
        LoanApplication application = context.loanService.createAndSubmit(1, 1, 10_000, "Seeds");
        context.loanService.addGuarantee(application.getId(), 2);
        assertThrows(IllegalArgumentException.class, () -> context.loanService.addGuarantee(application.getId(), 2));
    }

    @Test
    void requiredGuaranteesMoveApplicationToOfficerReview() {
        LoanApplication application = context.loanService.createAndSubmit(1, 1, 10_000, "Seeds");
        context.loanService.addGuarantee(application.getId(), 2);
        assertEquals(ApplicationStatus.GUARANTEE_PENDING, context.applications.findById(application.getId()).orElseThrow().getStatus());
        context.loanService.addGuarantee(application.getId(), 3);
        assertEquals(ApplicationStatus.OFFICER_REVIEW, context.applications.findById(application.getId()).orElseThrow().getStatus());
    }

    @Test
    void completeApprovalWorkflowEndsApproved() {
        LoanApplication application = context.loanService.createAndSubmit(1, 3, 5_000, "Medical cost");
        context.loanService.addGuarantee(application.getId(), 2);
        context.loanService.officerApprove(application.getId());
        context.loanService.managerApprove(application.getId(), "Approved by manager");
        assertEquals(ApplicationStatus.APPROVED, context.applications.findById(application.getId()).orElseThrow().getStatus());
    }

    @Test
    void ineligibleMemberCannotApply() throws SQLException {
        try (Connection connection = context.database.connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE members SET eligible=0 WHERE id=1");
        }
        assertThrows(IllegalStateException.class,
                () -> context.loanService.createAndSubmit(1, 3, 5_000, "Medical cost"));
    }
}
