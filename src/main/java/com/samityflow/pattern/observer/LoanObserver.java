package com.samityflow.pattern.observer;

/** Represents LoanObserver within the microfinance workflow. */
public interface LoanObserver {
    void update(String event);
}
