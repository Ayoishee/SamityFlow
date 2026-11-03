package com.samityflow.model;

/** Represents GroupUnit within the microfinance workflow. */
public record GroupUnit(int id, int samityId, String name, boolean active) {
    @Override
    public String toString() {
        return name;
    }
}
