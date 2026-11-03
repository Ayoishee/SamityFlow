package com.samityflow.model;

/** Represents Member within the microfinance workflow. */
public record Member(
        int id, int groupUnitId, String name, String phone, boolean eligible, boolean active) {
    @Override
    public String toString() {
        return name;
    }
}
