package com.smartscheduler.appointment.exception;

public class SlotUnavailableException extends RuntimeException {

    public SlotUnavailableException(String requestedTime) {
        super("The requested slot is already booked: " + requestedTime);
    }
}
