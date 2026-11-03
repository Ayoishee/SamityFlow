package com.samityflow.pattern.command;

/** Represents PaymentCommand within the microfinance workflow. */
public interface PaymentCommand {
    void execute();

    void undo();
}
