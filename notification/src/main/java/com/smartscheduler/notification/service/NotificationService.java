package com.smartscheduler.notification.service;

import com.smartscheduler.common.entity.*;
import com.smartscheduler.common.event.SlotBookedEvent;
import com.smartscheduler.common.repository.*;
import com.smartscheduler.notification.config.NotificationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class NotificationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMM dd");

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    @Autowired
    private WaitlistRepository waitlistRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RasaService rasaService;

    @Autowired
    private NotificationProperties notificationProperties;

    @Autowired
    private MessageService messageService;

    @RabbitListener(queues = "appointments.slots.booked.queue")
    public void onSlotBooked(SlotBookedEvent event) {
        log.info("Received SlotCancelledEvent: {}", event);

        try {
            appointmentRepository.findById(event.getAppointmentId()).ifPresent(appointment -> {
                ZonedDateTime patientTime = event.getStartTime().atZone(appointment.getPatient().getTimeZoneId());

                // 2. Format the date and time strings
                String formattedDate = patientTime.format(DATE_FORMATTER);
                String formattedTime = patientTime.format(TIME_FORMATTER);

                // 3. Construct the final string using String.format() or template literal
                String messageTemplate = "CONFIRMED: Your %s appointment is booked for %s at %s with %s. We look forward to seeing you!";

                String finalMessage = String.format(
                        messageTemplate,
                        appointment.getDoctor().getSpecialization(),
                        formattedDate,
                        formattedTime,
                        appointment.getDoctor().getName()
                );
                messageService.sendWhatsAppMessage(List.of(Map.of("recipient_id", appointment.getPatient().getPhone(), "text", finalMessage)));
            });
        } catch (Exception ex) {
            log.error("Exception while processing SlotCancelledEvent", ex);
        }
    }

}