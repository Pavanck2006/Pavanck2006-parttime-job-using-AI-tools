package com.parttimejob.enums;

public enum WorkType {
    FOOD_SERVER("Food Server"),
    CATERING_HELPER("Catering Helper"),
    KITCHEN_HELPER("Kitchen Helper"),
    CLEANER("Cleaner"),
    EVENT_HELPER("Event Helper"),
    WAITER("Waiter"),
    OTHER("Other");

    private final String displayName;

    WorkType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
