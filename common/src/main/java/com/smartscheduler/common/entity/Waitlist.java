package com.smartscheduler.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "waitlist")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Waitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Patient patient;

    @ManyToOne
    private Doctor doctor;

    @OneToOne
    private Appointment appointment;

    @ElementCollection
    @CollectionTable(name = "waitlist_preferred_dates", joinColumns = @JoinColumn(name = "waitlist_id"))
    @Column(name = "preferred_date")
    private List<Instant> preferredDates;

    private Boolean active;

    private Integer consecutiveMisses = 0;

    private Instant createdAt;

    private Instant optOutExpiry;

    private Boolean notified;

}
