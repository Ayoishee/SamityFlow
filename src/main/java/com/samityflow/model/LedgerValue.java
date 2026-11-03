package com.samityflow.model;

/** Shared persisted ledger vocabulary; values remain compatible with existing SQLite rows. */
public final class LedgerValue {
    private LedgerValue() {}

    public static final String ACTIVE = "ACTIVE";
    public static final String DEFAULTED = "DEFAULTED";
    public static final String COMPLETED = "COMPLETED";
    public static final String PENDING = "PENDING";
    public static final String PARTIALLY_PAID = "PARTIALLY_PAID";
    public static final String PAID = "PAID";
    public static final String OVERDUE = "OVERDUE";
    public static final String REVERSED = "REVERSED";
    public static final String REVERSAL = "REVERSAL";
    public static final String DEPOSIT = "DEPOSIT";
    public static final String WITHDRAWAL = "WITHDRAWAL";
}
