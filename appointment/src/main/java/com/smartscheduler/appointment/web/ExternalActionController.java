package com.smartscheduler.appointment.web;

import com.smartscheduler.appointment.dto.AppointmentResponseDto;
import com.smartscheduler.appointment.dto.DenyRequestDto;
import com.smartscheduler.appointment.dto.RescheduleRequestDto;
import com.smartscheduler.appointment.dto.RescheduleResponseDto;
import com.smartscheduler.appointment.exception.AppointmentNotFoundException;
import com.smartscheduler.appointment.exception.SlotUnavailableException;
import com.smartscheduler.appointment.service.ExternalActionService;
import com.smartscheduler.common.entity.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/external")
public class ExternalActionController {

    private static final Logger logger = LoggerFactory.getLogger(ExternalActionController.class);
    private final ExternalActionService externalActionService;

    public ExternalActionController(ExternalActionService externalActionService) {
        this.externalActionService = externalActionService;
    }

    @GetMapping("/by-phone/{phoneNumber}")
    public ResponseEntity<Map<String, List<AppointmentResponseDto>>> fetchAppointments(@PathVariable String phoneNumber) {
        logger.info("External request to get appointments for phone number: {}", phoneNumber);

        List<Appointment> entities = externalActionService.getAppointmentsByPhoneNumber(phoneNumber);

        List<AppointmentResponseDto> rasaAppointments = entities.stream()
                .map(AppointmentResponseDto::new)
                .toList();

        Map<String, List<AppointmentResponseDto>> response = Map.of("appointments", rasaAppointments);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDto> getAppointment(@PathVariable String id) {
        try {
            logger.info("External request to get appointment details for ID: {}", id);
            Appointment appointment = externalActionService.findById(id);

            AppointmentResponseDto response = new AppointmentResponseDto(appointment);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (AppointmentNotFoundException e) {
            logger.warn("Appointment not found for ID: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<RescheduleResponseDto> rescheduleAppointment(@PathVariable String id, @RequestBody RescheduleRequestDto request) {
        try {
            logger.info("External request to reschedule oldId={} to newTime={}", id, request.getNewDatetime());

            externalActionService.reschedule(Long.valueOf(request.getSlotOfferId()), Long.valueOf(id), request.getNewDatetime());

            return new ResponseEntity<>(new RescheduleResponseDto("SUCCESS"), HttpStatus.OK);
        } catch (SlotUnavailableException e) {
            logger.warn("Reschedule failed due to conflict: {}", e.getMessage());
            return new ResponseEntity<>(new RescheduleResponseDto("SLOT_TAKEN"), HttpStatus.CONFLICT);
        } catch (AppointmentNotFoundException e) {
            logger.warn("Reschedule failed: Appointment not found with ID {}.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request during reschedule for ID {}: {}", id, e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{id}/deny-offer")
    @ResponseStatus(HttpStatus.OK)
    public void denyOffer(@PathVariable String id, @RequestBody DenyRequestDto denyRequestDto) {
        logger.info("External action: Slot offer denied for old ID: {}", id);
        externalActionService.denyOfferStatus(Long.valueOf(denyRequestDto.getSlotOfferId()), Long.valueOf(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void cancelAppointment(@PathVariable String id) {
        logger.info("External request to cancel appointment ID: {}", id);
        externalActionService.cancel(id);
    }
}