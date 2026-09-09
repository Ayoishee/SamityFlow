package com.samityflow.pattern.state;

import com.samityflow.model.ApplicationStatus;
import com.samityflow.model.LoanApplication;

public final class ApplicationStates {
    private ApplicationStates() { }

    public static ApplicationState forStatus(ApplicationStatus status) {
        return switch (status) {
            case DRAFT -> new DraftState();
            case GUARANTEE_PENDING -> new GuaranteePendingState();
            case OFFICER_REVIEW -> new OfficerReviewState();
            case MANAGER_REVIEW -> new ManagerReviewState();
            case APPROVED -> new ApprovedState();
            case REJECTED -> new RejectedState();
        };
    }

    private static class DraftState implements ApplicationState {
        public void submit(LoanApplication a) { a.changeStatus(ApplicationStatus.GUARANTEE_PENDING); }
    }

    private static class GuaranteePendingState implements ApplicationState {
        public void guaranteesCompleted(LoanApplication a) { a.changeStatus(ApplicationStatus.OFFICER_REVIEW); }
        public void reject(LoanApplication a, String comment) { rejectNow(a, comment); }
    }

    private static class OfficerReviewState implements ApplicationState {
        public void officerApprove(LoanApplication a) { a.markOfficerApproved(); a.changeStatus(ApplicationStatus.MANAGER_REVIEW); }
        public void reject(LoanApplication a, String comment) { rejectNow(a, comment); }
    }

    private static class ManagerReviewState implements ApplicationState {
        public void managerApprove(LoanApplication a, String comment) { a.setManagerComment(comment); a.changeStatus(ApplicationStatus.APPROVED); }
        public void reject(LoanApplication a, String comment) { rejectNow(a, comment); }
    }

    private static class ApprovedState implements ApplicationState { }
    private static class RejectedState implements ApplicationState { }

    private static void rejectNow(LoanApplication a, String comment) {
        a.setManagerComment(comment);
        a.changeStatus(ApplicationStatus.REJECTED);
    }
}
