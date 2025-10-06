package com.smartscheduler.appointment.payload;

import lombok.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BookAppointmentPayload {

    private static final DateTimeFormatter SLOT_ALERT_PREFERRED_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    private String bookingId;

    // Slot details
    private String startTimeIso;
    private String endTimeIso;
    private String timeZone;
    private String duration;

    // Patient info
    private String patientName;
    private String patientEmail;
    private String patientPhone;

    private Object isWhatsappNumber;

    // Staff info (doctor)
    private String staffId;
    private String staffName;
    private String staffEmail;
    private String staffPhone;
    private String staffSpecialization;

    private String notes;

    private String slotAlertPreferredDate1;
    private String slotAlertPreferredDate2;
    private String slotAlertPreferredDate3;

    public Boolean getIsWhatsappNumber() {
        if (isWhatsappNumber instanceof String s) {
            return false;
        }
        List<String> values = (List<String>) isWhatsappNumber;
        return "YES".equalsIgnoreCase(values.getFirst());
    }

    public List<String> getSlotAlertPreferredDates() {
        List<String> slotAlertPreferredDates = new ArrayList<>();
        if(!slotAlertPreferredDate1.isBlank()) {
            slotAlertPreferredDates.add(slotAlertPreferredDate1);
        }
        if(!slotAlertPreferredDate2.isBlank()) {
            slotAlertPreferredDates.add(slotAlertPreferredDate2);
        }
        if(!slotAlertPreferredDate3.isBlank()) {
            slotAlertPreferredDates.add(slotAlertPreferredDate1);
        }
        return slotAlertPreferredDates;
    }

}
