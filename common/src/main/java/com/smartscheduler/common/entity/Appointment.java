package com.smartscheduler.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String zohoId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    private Instant startTime;
    private Instant endTime;

    @Convert(converter = StatusConverter.class)
    private Status status;

    @Convert(converter = SourceConverter.class)
    private Source source;

    private Instant createdAt;
    private Instant updatedAt;

    private Boolean isWhatsappNumber;

    public LocalDateTime getStartTimeLocal() {
        return LocalDateTime.ofInstant(startTime, ZoneId.of("UTC"));
    }

    public LocalDateTime getEndTimeLocal() {
        return LocalDateTime.ofInstant(endTime, ZoneId.of("UTC"));
    }

    public enum Status {
        UPCOMING, CANCELLED, COMPLETED, RESCHEDULED
    }

    public enum Source {
        ZOHO, MANUAL
    }

    @Converter(autoApply = true)
    public static class StatusConverter implements AttributeConverter<Status, String> {

        @Override
        public String convertToDatabaseColumn(Status status) {
            return status == null ? null : status.name();
        }

        @Override
        public Status convertToEntityAttribute(String dbData) {
            if (dbData == null) return null;
            return Status.valueOf(dbData.trim().toUpperCase());
        }
    }

    @Converter(autoApply = true)
    public static class SourceConverter implements AttributeConverter<Source, String> {

        @Override
        public String convertToDatabaseColumn(Source source) {
            return source == null ? null : source.name();
        }

        @Override
        public Source convertToEntityAttribute(String dbData) {
            if (dbData == null) return null;
            return Source.valueOf(dbData.trim().toUpperCase());
        }
    }
}
