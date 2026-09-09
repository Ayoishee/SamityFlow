package com.samityflow;

import com.samityflow.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StateTest {
    @Test
    void validApplicationTransitionsReachApproved() {
        LoanApplication application = app(ApplicationStatus.DRAFT);
        application.submit();
        application.guaranteesCompleted();
        application.officerApprove();
        application.managerApprove("Looks good");
        assertEquals(ApplicationStatus.APPROVED, application.getStatus());
    }

    @Test
    void draftCannotSkipDirectlyToManagerApproval() {
        LoanApplication application = app(ApplicationStatus.DRAFT);
        assertThrows(IllegalStateException.class, () -> application.managerApprove("skip"));
        assertEquals(ApplicationStatus.DRAFT, application.getStatus());
    }

    @Test
    void rejectedApplicationCannotMoveAgain() {
        LoanApplication application = app(ApplicationStatus.OFFICER_REVIEW);
        application.reject("Risk is too high");
        assertThrows(IllegalStateException.class, application::officerApprove);
    }

    private LoanApplication app(ApplicationStatus status) {
        return new LoanApplication(1, 1, 1, 10_000, "Farm", status, false, "");
    }
}
