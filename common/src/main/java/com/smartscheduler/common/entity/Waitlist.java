package com.smartscheduler.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    @OneToMany(mappedBy = "waitlist", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<WaitlistPreferredDate> preferredDates = new ArrayList<>();

    private Boolean active;

    private Integer consecutiveMisses = 0;

    private Instant createdAt;

    private Instant optOutExpiry;

    private Boolean notified;

    public void addPreferredDate(WaitlistPreferredDate preferredDate) {
        this.preferredDates.add(preferredDate);
        preferredDate.setWaitlist(this);
    }

    @Override
    public String toString() {
        return "Waitlist";
    }

}
