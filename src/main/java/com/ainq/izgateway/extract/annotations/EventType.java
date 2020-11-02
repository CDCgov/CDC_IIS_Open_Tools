package com.ainq.izgateway.extract.annotations;

public enum EventType {
    VACCINATION("Vaccination"), REFUSAL("Refusal"), MISSED_APPOINTMENT("Missed Appointment");
    private String display;
    private EventType(String display) {
        this.display = display;
    }
    public String toString() {
        return display;
    }
}
