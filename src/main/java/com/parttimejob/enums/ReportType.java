package com.parttimejob.enums;

public enum ReportType {
    PAYMENT_NOT_RECEIVED("Payment Not Received"),
    PAYMENT_PARTIALLY_RECEIVED("Payment Partially Received"),
    OWNER_BEHAVIOUR("Owner Misbehavior / Harassment"),
    FAKE_JOB("Fake / Misleading Job"),
    WRONG_LOCATION("Incorrect / Misrepresented Location"),
    OTHER("Other Issue");

    private final String displayName;

    ReportType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
