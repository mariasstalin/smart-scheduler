package com.smartscheduler.appointment.exception;

public class AppointmentNotFoundException extends RuntimeException {

    public AppointmentNotFoundException(String id) {
        super("Appointment not found with ID: " + id);
    }
}
