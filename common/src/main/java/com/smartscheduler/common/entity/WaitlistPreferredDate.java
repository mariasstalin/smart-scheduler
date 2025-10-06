package com.smartscheduler.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Entity
@Table(name = "waitlist_preferred_dates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitlistPreferredDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "waitlist_id", nullable = false)
    private Waitlist waitlist;

    @Column(name = "preferred_date", nullable = false)
    private Instant preferredDate;

    public WaitlistPreferredDate(Instant preferredDate) {
        this.preferredDate = preferredDate;
    }

    public LocalDate getPreferredDateLocal() {
        return LocalDate.ofInstant(preferredDate, ZoneId.of("UTC"));
    }
}
