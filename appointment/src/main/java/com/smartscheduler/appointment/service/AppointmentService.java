
package com.smartscheduler.appointment.service;

import com.smartscheduler.appointment.model.Appointment;
import com.smartscheduler.appointment.repo.AppointmentRepository;
import com.smartscheduler.appointment.zoho.ZohoClient;
import com.smartscheduler.common.dto.AppointmentMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AppointmentService {
    private final AppointmentRepository repo;
    private final ZohoClient zohoClient;
    private final RabbitTemplate rabbit;

    public AppointmentService(AppointmentRepository repo, RabbitTemplate rabbit, ZohoClient zohoClient) {
        this.zohoClient = zohoClient;
        this.repo = repo;
        this.rabbit = rabbit;
    }

    public Appointment create(Appointment ap) {
        Appointment saved = repo.save(ap);
        AppointmentMessage msg = new AppointmentMessage(saved.getId(), saved.getUserId(), "CREATED", saved.getStartTime(), saved.getEndTime());

        try {
            Map zohoPayload = new java.util.HashMap();
            zohoPayload.put("start_time", saved.getStartTime().toString());
            zohoPayload.put("end_time", saved.getEndTime().toString());
            zohoPayload.put("customer", java.util.Map.of("email", "demo@customer.com"));
            var zohoResp = zohoClient.createBooking(zohoPayload);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        rabbit.convertAndSend("appointments-exchange", "appointment.created", msg);
        return saved;
    }

    public Optional<Appointment> findById(Long id) {
        return repo.findById(id);
    }

    public List<Appointment> listAll() {
        return repo.findAll();
    }
}
