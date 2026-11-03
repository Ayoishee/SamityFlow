package com.samityflow.model;

/** Represents Samity within the microfinance workflow. */
public record Samity(int id, String name, String meetingDay, boolean active) {
    @Override
    public String toString() {
        return name;
    }
}
