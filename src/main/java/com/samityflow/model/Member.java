package com.samityflow.model;

public record Member(int id, int groupUnitId, String name, String phone, boolean eligible, boolean active) {
    @Override
    public String toString() {
        return name;
    }
}
