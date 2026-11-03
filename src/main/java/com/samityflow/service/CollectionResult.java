package com.samityflow.service;

/** Represents CollectionResult within the microfinance workflow. */
public class CollectionResult {
    private final boolean success;
    private final String message;

    public CollectionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
