package com.samityflow.model;

public record Samity(int id, String name, String meetingDay, boolean active) {
    @Override
    public String toString() {
        return name;
    }
}
