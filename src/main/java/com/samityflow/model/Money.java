package com.samityflow.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Currency uses two decimal places, HALF_UP rounding, and exact integer cents for savings. */
public final class Money {
    private Money() {}

    public static BigDecimal round(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static long cents(BigDecimal amount) {
        return round(amount).movePointRight(2).longValueExact();
    }

    public static BigDecimal decimal(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}
