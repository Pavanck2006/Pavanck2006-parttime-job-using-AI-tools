package com.parttimejob.enums;

public enum PaymentType {
    ON_SPOT_PAYMENT("On-Spot Payment"),
    AFTER_WORK("After Work"),
    OTHER("Other");

    private final String displayName;

    PaymentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
