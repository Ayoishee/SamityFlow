package com.samityflow.model;

public record GroupUnit(int id, int samityId, String name, boolean active) {
    @Override
    public String toString() {
        return name;
    }
}
