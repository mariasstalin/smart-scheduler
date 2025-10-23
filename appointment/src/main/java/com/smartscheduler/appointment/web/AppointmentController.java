package com.smartscheduler.appointment.web;

import com.smartscheduler.appointment.dto.AppointmentResponseDto;
import com.smartscheduler.appointment.dto.RescheduleRequestDto;
import com.smartscheduler.appointment.dto.RescheduleResponseDto;
import com.smartscheduler.appointment.exception.AppointmentNotFoundException;
import com.smartscheduler.appointment.exception.SlotUnavailableException;
import com.smartscheduler.appointment.service.AppointmentService;
import com.smartscheduler.common.entity.Appointment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/user/{phoneNumber}")
    public ResponseEntity<Map<String, List<AppointmentResponseDto>>> getUserAppointments(@PathVariable String phoneNumber) {

        List<Appointment> entities = appointmentService.getAppointmentsByPhoneNumber(phoneNumber);

        List<AppointmentResponseDto> rasaAppointments = entities.stream()
                .map(AppointmentResponseDto::new)
                .toList();

        Map<String, List<AppointmentResponseDto>> response = Map.of("appointments", rasaAppointments);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<AppointmentResponseDto> getAppointmentDetails(@PathVariable String appointmentId) {
        try {
            Appointment appointment = appointmentService.findById(appointmentId);

            AppointmentResponseDto response = new AppointmentResponseDto(appointment);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (AppointmentNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/reschedule")
    public ResponseEntity<RescheduleResponseDto> rescheduleAppointment(@RequestBody RescheduleRequestDto request) {
        try {
            Appointment newAppointment = appointmentService.reschedule(
                    request.getOldAppointmentId(),
                    request.getNewDatetime()
            );

            RescheduleResponseDto response = new RescheduleResponseDto(
                    "SUCCESS",
                    String.valueOf(newAppointment.getId()),
                    newAppointment.getStartTimeLocal().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            );
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (SlotUnavailableException e) {
            return new ResponseEntity<>(new RescheduleResponseDto("SLOT_TAKEN", null, null), HttpStatus.CONFLICT);

        } catch (AppointmentNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/offer/{oldId}/deny")
    @ResponseStatus(HttpStatus.OK)
    public void denySlotOffer(@PathVariable String oldId) {
        appointmentService.denyOfferStatus(oldId);
    }

    @DeleteMapping("/{appointmentId}")
    @ResponseStatus(HttpStatus.OK)
    public void cancelAppointment(@PathVariable String appointmentId) {
        appointmentService.cancel(appointmentId);
    }
}