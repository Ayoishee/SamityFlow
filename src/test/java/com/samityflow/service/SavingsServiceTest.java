package com.samityflow.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SavingsServiceTest {

    @Test
    void acceptsWithdrawalWithinAvailableBalance() {
        SavingsService service = new SavingsService();

        assertTrue(service.validateWithdrawal(1_000, 600));
        assertTrue(service.validateWithdrawal(1_000, 1_000));
    }

    @Test
    void rejectsWithdrawalAboveAvailableBalance() {
        SavingsService service = new SavingsService();

        assertFalse(service.validateWithdrawal(1_000, 1_001));
    }

    @Test
    void rejectsNegativeWithdrawal() {
        SavingsService service = new SavingsService();

        assertFalse(service.validateWithdrawal(1_000, -1));
    }
}
