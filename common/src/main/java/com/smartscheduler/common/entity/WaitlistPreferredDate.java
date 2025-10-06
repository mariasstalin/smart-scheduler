package com.smartscheduler.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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
    private LocalDate preferredDate;
}
