package com.samityflow.pattern.state;

import com.samityflow.model.LoanApplication;

public interface ApplicationState {
    default void submit(LoanApplication application) { invalid(application); }
    default void guaranteesCompleted(LoanApplication application) { invalid(application); }
    default void officerApprove(LoanApplication application) { invalid(application); }
    default void managerApprove(LoanApplication application, String comment) { invalid(application); }
    default void reject(LoanApplication application, String comment) { invalid(application); }

    private void invalid(LoanApplication application) {
        throw new IllegalStateException("Action is not allowed while application is " + application.getStatus());
    }
}
