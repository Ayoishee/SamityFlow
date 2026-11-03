package com.samityflow.model;

/** Represents ApplicationStatus within the microfinance workflow. */
public enum ApplicationStatus {
    DRAFT,
    GUARANTEE_PENDING,
    OFFICER_REVIEW,
    MANAGER_REVIEW,
    APPROVED,
    REJECTED
}
